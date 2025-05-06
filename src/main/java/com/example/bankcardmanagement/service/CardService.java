package com.example.bankcardmanagement.service;

import com.example.bankcardmanagement.dto.request.CreateCardRequest;
import com.example.bankcardmanagement.entity.Card;
import com.example.bankcardmanagement.enums.CardStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;

public interface CardService {

    Page<Card> findCardsByCurrentUser(Pageable pageable, CardStatus status);

    Card findCardByIdAndCurrentUser(Long cardId);

    BigDecimal getCardBalance(Long cardId);

    Card requestBlockCard(Long cardId);

    void transferBetweenOwnCards(Long fromCardId, Long toCardId, BigDecimal amount);

    Page<Card> findAllCards(Pageable pageable, Long ownerId, CardStatus status);

    Card createCard(CreateCardRequest createCardRequest);

    Card findCardById_Admin(Long cardId);

    Card updateCardStatus_Admin(Long cardId, CardStatus newStatus);

    void deleteCard_Admin(Long cardId);

    String getDecryptedCardNumber(Card card);

    void checkAndExpireCards();
}