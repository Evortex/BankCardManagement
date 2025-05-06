package com.example.bankcardmanagement.service.impl;

import com.example.bankcardmanagement.dto.request.CreateCardRequest;
import com.example.bankcardmanagement.entity.Card;
import com.example.bankcardmanagement.entity.YearMonthAttributeConverter;
import com.example.bankcardmanagement.entity.User;
import com.example.bankcardmanagement.enums.CardStatus;
import com.example.bankcardmanagement.exception.BadRequestException;
import com.example.bankcardmanagement.exception.CardOperationException;
import com.example.bankcardmanagement.exception.InsufficientFundsException;
import com.example.bankcardmanagement.exception.ResourceNotFoundException;
import com.example.bankcardmanagement.repository.CardRepository;
import com.example.bankcardmanagement.repository.UserRepository;
import com.example.bankcardmanagement.service.CardService;
import com.example.bankcardmanagement.service.EncryptionService;
import com.example.bankcardmanagement.service.UserService;
import com.example.bankcardmanagement.util.CardMaskingUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CardServiceImpl implements CardService {

    private static final Logger logger = LoggerFactory.getLogger(CardServiceImpl.class);

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final EncryptionService encryptionService;

    @Override
    @Transactional(readOnly = true)
    public Page<Card> findCardsByCurrentUser(Pageable pageable, CardStatus status) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        logger.debug("Поиск карт для пользователя ID: {} с фильтром status: {}", currentUser.getId(), status);

        Specification<Card> spec = Specification.where(CardSpecification.hasOwnerId(currentUser.getId()));
        if (status != null) {
            spec = spec.and(CardSpecification.hasStatus(status));
        }
        return cardRepository.findAll(spec, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Card findCardByIdAndCurrentUser(Long cardId) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        logger.debug("Поиск карты ID: {} для пользователя ID: {}", cardId, currentUser.getId());
        return cardRepository.findByIdAndOwnerId(cardId, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Карта не найдена " +
                        "или не принадлежит пользователю, ID: " + cardId));
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getCardBalance(Long cardId) {
        Card card = findCardByIdAndCurrentUser(cardId);
        logger.debug("Запрос баланса для карты ID: {}", cardId);
        return card.getBalance();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.SERIALIZABLE)
    public Card requestBlockCard(Long cardId) {
        Card card = findCardByIdAndCurrentUser(cardId);
        logger.info("Запрос на блокировку карты ID: {} от пользователя ID: {}", cardId, card.getOwner().getId());

        if (card.getStatus() == CardStatus.BLOCKED) {
            throw new CardOperationException("Карта ID: " + cardId + " уже заблокирована.");
        }
        if (card.getStatus() == CardStatus.EXPIRED) {
            throw new CardOperationException("Нельзя заблокировать карту ID: "
                    + cardId + " с истекшим сроком действия.");
        }
        card.checkAndUpdateExpiryStatus();
        if (card.getStatus() == CardStatus.EXPIRED){
            throw new CardOperationException("Срок действия карты ID: " + cardId + " истек.");
        }

        card.setStatus(CardStatus.BLOCKED);
        Card updatedCard = cardRepository.save(card);
        logger.info("Карта ID: {} успешно заблокирована", cardId);
        return updatedCard;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.SERIALIZABLE)
    public void transferBetweenOwnCards(Long fromCardId, Long toCardId, BigDecimal amount) {
        if (Objects.equals(fromCardId, toCardId)) {
            throw new BadRequestException("Нельзя перевести средства на ту же самую карту.");
        }
        User currentUser = userService.getCurrentAuthenticatedUser();
        Long currentUserId = currentUser.getId();
        logger.info("Попытка перевода {} с карты ID: {} на карту ID: {} для пользователя ID: {}",
                amount, fromCardId, toCardId, currentUserId);
        Card fromCard = cardRepository.findByIdAndOwnerId(fromCardId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Карта-отправитель ID: "
                        + fromCardId + " не найдена или не принадлежит пользователю."));
        Card toCard = cardRepository.findByIdAndOwnerId(toCardId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Карта-получатель ID: "
                        + toCardId + " не найдена или не принадлежит пользователю."));
        fromCard.checkAndUpdateExpiryStatus();

        if (!fromCard.isActive()) {
            throw new CardOperationException("Карта-отправитель ID: "
                    + fromCardId + " неактивна (статус: " + fromCard.getStatus() + "). Перевод невозможен.");
        }

        toCard.checkAndUpdateExpiryStatus();
        if (toCard.getStatus() == CardStatus.EXPIRED || toCard.getStatus() == CardStatus.BLOCKED) {
            throw new CardOperationException("Карта-получатель ID: " + toCardId + " неактивна (статус: "
                    + toCard.getStatus() + "). Перевод невозможен.");
        }

        if (fromCard.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Недостаточно средств на карте-отправителе ID: " + fromCardId);
        }

        fromCard.setBalance(fromCard.getBalance().subtract(amount));
        toCard.setBalance(toCard.getBalance().add(amount));

        cardRepository.save(fromCard);
        cardRepository.save(toCard);

        logger.info("Перевод {} с карты ID: {} на карту ID: {} успешно выполнен.", amount, fromCardId, toCardId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Card> findAllCards(Pageable pageable, Long ownerId, CardStatus status) {
        logger.debug("Администратор запрашивает все карты. Фильтры: ownerId={}, status={}", ownerId, status);
        Specification<Card> spec = Specification.where(null);
        if (ownerId != null) {
            spec = spec.and(CardSpecification.hasOwnerId(ownerId));
        }
        if (status != null) {
            spec = spec.and(CardSpecification.hasStatus(status));
        }
        return cardRepository.findAll(spec, pageable);
    }

    @Override
    @Transactional
    public Card createCard(CreateCardRequest request) {
        logger.info("Администратор создает карту для пользователя ID: {}", request.getOwnerId());
        User owner = userRepository.findById(request.getOwnerId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getOwnerId()));
        String encryptedCardNumber = encryptionService.encrypt(request.getCardNumber());

        if (cardRepository.findByCardNumberEncrypted(encryptedCardNumber).isPresent()) {
            String maskedNumber = CardMaskingUtil.maskCardNumber(request.getCardNumber());
            logger.warn("Попытка создания карты с уже существующим номером (маскированный): {}", maskedNumber);
            throw new BadRequestException("Карта с таким номером уже существует.");
        }

        String cardHolderName = (owner.getFirstName() + " " + owner.getLastName()).toUpperCase();
        Card newCard = new Card(
                encryptedCardNumber,
                owner,
                cardHolderName,
                request.getExpiryDate()
        );
        newCard.setStatus(CardStatus.ACTIVE);
        newCard.setBalance(BigDecimal.ZERO);
        Card savedCard = cardRepository.save(newCard);
        logger.info("Карта ID: {} успешно создана для пользователя ID: {}", savedCard.getId(), owner.getId());
        return savedCard;
    }

    @Override
    @Transactional(readOnly = true)
    public Card findCardById_Admin(Long cardId) {
        logger.debug("Администратор запрашивает карту ID: {}", cardId);
        return cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card", "id", cardId));
    }

    @Override
    @Transactional
    public Card updateCardStatus_Admin(Long cardId, CardStatus newStatus) {
        logger.info("Администратор обновляет статус карты ID: {} на {}", cardId, newStatus);
        Card card = findCardById_Admin(cardId);
        card.checkAndUpdateExpiryStatus();

        if (card.getStatus() == CardStatus.EXPIRED && newStatus == CardStatus.ACTIVE) {
            throw new CardOperationException("Нельзя активировать карту ID: " + cardId + " с истекшим сроком действия.");
        }

        if (card.getStatus() == newStatus) {
            logger.warn("Статус карты ID: {} уже {}", cardId, newStatus);
            return card;
        }

        card.setStatus(newStatus);
        Card updatedCard = cardRepository.save(card);
        logger.info("Статус карты ID: {} успешно обновлен на {}", cardId, newStatus);
        return updatedCard;
    }

    @Override
    @Transactional
    public void deleteCard_Admin(Long cardId) {
        logger.info("Администратор удаляет карту ID: {}", cardId);
        Card card = findCardById_Admin(cardId);
        cardRepository.delete(card);
        logger.info("Карта ID: {} успешно удалена.", cardId);
    }

    @Override
    public String getDecryptedCardNumber(Card card) {
        if (card == null || card.getCardNumberEncrypted() == null) {
            return null;
        }
        try {
            return encryptionService.decrypt(card.getCardNumberEncrypted());
        } catch (Exception e) {
            logger.error("Не удалось дешифровать номер карты ID: {}", card.getId(), e);
            return null;
        }
    }

    @Override
    @Transactional
    @Scheduled(cron = "${app.card.expiry-check.cron:0 0 1 * * ?}")
    public void checkAndExpireCards() {
        logger.info("Запуск плановой проверки истекших карт...");
        YearMonth currentMonth = YearMonth.now();
        Specification<Card> spec = Specification.where(CardSpecification.hasStatus(CardStatus.ACTIVE))
                .and(CardSpecification.expiryDateBefore(currentMonth));

        List<Card> cardsToExpire = cardRepository.findAll(spec);

        if (cardsToExpire.isEmpty()) {
            logger.info("Не найдено активных карт с истекшим сроком действия.");
            return;
        }

        logger.info("Найдено {} карт для установки статуса EXPIRED.", cardsToExpire.size());
        for (Card card : cardsToExpire) {
            card.setStatus(CardStatus.EXPIRED);
            cardRepository.save(card);
            logger.debug("Статус карты ID: {} установлен в EXPIRED.", card.getId());
        }
        logger.info("Плановая проверка истекших карт завершена.");
    }

    private static class CardSpecification {
        public static Specification<Card> hasOwnerId(Long ownerId) {
            return (root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("owner").get("id"), ownerId);
        }

        public static Specification<Card> hasStatus(CardStatus status) {
            return (root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("status"), status);
        }
        public static Specification<Card> expiryDateBefore(YearMonth month) {
            String monthString = month.format(YearMonthAttributeConverter.FORMATTER);
            return (root, query, criteriaBuilder) ->
                    criteriaBuilder.lessThan(root.get("expiryDate").as(String.class), monthString);
        }
    }
}
