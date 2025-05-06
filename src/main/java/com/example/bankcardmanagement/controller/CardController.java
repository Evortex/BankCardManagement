package com.example.bankcardmanagement.controller;

import com.example.bankcardmanagement.dto.request.CreateCardRequest;
import com.example.bankcardmanagement.dto.request.TransferRequest;
import com.example.bankcardmanagement.dto.request.UpdateCardStatusRequest;
import com.example.bankcardmanagement.dto.response.CardResponse;
import com.example.bankcardmanagement.dto.response.PageResponse;
import com.example.bankcardmanagement.entity.Card;
import com.example.bankcardmanagement.enums.CardStatus;
import com.example.bankcardmanagement.mapper.CardMapper;
import com.example.bankcardmanagement.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Управление картами", description = "API для операций с банковскими картами")
@SecurityRequirement(name = "bearerAuth")
public class CardController {

    private final CardService cardService;
    private final CardMapper cardMapper;

    @Operation(summary = "Получить список своих карт",
            description = "Возвращает пагинированный список карт" +
            " текущего пользователя с возможностью фильтрации по статусу.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список карт получен",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Необходима аутентификация"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен")
    })
    @GetMapping("/cards")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PageResponse<CardResponse>> getCurrentUserCards(
            @PageableDefault(sort = "createdAt") Pageable pageable,
            @Parameter(description = "Фильтр по статусу карты (ACTIVE, BLOCKED, EXPIRED)")
            @RequestParam(required = false) CardStatus status) {

        Page<Card> cardPage = cardService.findCardsByCurrentUser(pageable, status);
        List<CardResponse> cardResponseList = cardMapper.toCardResponseList(cardPage.getContent());
        PageResponse<CardResponse> response = PageResponse.fromPage(cardPage, cardResponseList);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Получить детали своей карты", description = "Возвращает детали конкретной карты по ID, " +
            "если она принадлежит текущему пользователю.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Карта найдена",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CardResponse.class))),
            @ApiResponse(responseCode = "401", description = "Необходима аутентификация"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен (не ваша карта)"),
            @ApiResponse(responseCode = "404", description = "Карта не найдена")
    })
    @GetMapping("/cards/{cardId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CardResponse> getCurrentUserCardById(@PathVariable Long cardId) {
        Card card = cardService.findCardByIdAndCurrentUser(cardId);
        return ResponseEntity.ok(cardMapper.toCardResponse(card));
    }

    @Operation(summary = "Получить баланс своей карты",
            description = "Возвращает баланс конкретной карты по ID, если она принадлежит текущему пользователю.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Баланс получен",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(type = "number", format = "double"))),
            @ApiResponse(responseCode = "401", description = "Необходима аутентификация"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен (не ваша карта)"),
            @ApiResponse(responseCode = "404", description = "Карта не найдена")
    })
    @GetMapping("/cards/{cardId}/balance")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<BigDecimal> getCurrentUserCardBalance(@PathVariable Long cardId) {
        BigDecimal balance = cardService.getCardBalance(cardId);
        return ResponseEntity.ok(balance);
    }

    @Operation(summary = "Запросить блокировку своей карты",
            description = "Пользователь инициирует блокировку своей карты.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Карта успешно заблокирована",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CardResponse.class))),
            @ApiResponse(responseCode = "401", description = "Необходима аутентификация"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен (не ваша карта)"),
            @ApiResponse(responseCode = "404", description = "Карта не найдена"),
            @ApiResponse(responseCode = "409",
                    description = "Операция невозможна (карта уже заблокирована/просрочена)")
    })
    @PostMapping("/cards/{cardId}/block-request")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CardResponse> requestBlockCard(@PathVariable Long cardId) {
        Card updatedCard = cardService.requestBlockCard(cardId);
        return ResponseEntity.ok(cardMapper.toCardResponse(updatedCard));
    }

    @Operation(summary = "Перевод средств между своими картами", description = "Переводит указанную" +
            " сумму с одной карты текущего пользователя на другую.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Перевод успешно выполнен"),
            @ApiResponse(responseCode = "400", description = "Некорректные данные запроса (например," +
                    " отрицательная сумма, недостаточно средств)"),
            @ApiResponse(responseCode = "401", description = "Необходима аутентификация"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен (карты не " +
                    "принадлежат пользователю или перевод между разными пользователями)"),
            @ApiResponse(responseCode = "404", description = "Одна из карт не найдена"),
            @ApiResponse(responseCode = "409",
                    description = "Операция невозможна (карта-отправитель заблокирована/просрочена)")
    })
    @PostMapping("/cards/transfer")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> transferBetweenOwnCards(@Valid @RequestBody TransferRequest transferRequest) {
        cardService.transferBetweenOwnCards(transferRequest.getFromCardId(),
                transferRequest.getToCardId(), transferRequest.getAmount());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Получить все карты (Админ)",
            description = "Возвращает пагинированный список всех карт в системе с возможностью фильтрации.")
    @ApiResponses()
    @GetMapping("/admin/cards")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<CardResponse>> getAllCards(
            @PageableDefault(size = 20, sort = "id") Pageable pageable,
            @Parameter(description = "Фильтр по ID владельца") @RequestParam(required = false) Long ownerId,
            @Parameter(description = "Фильтр по статусу карты") @RequestParam(required = false) CardStatus status) {

        Page<Card> cardPage = cardService.findAllCards(pageable, ownerId, status);
        List<CardResponse> cardResponseList = cardMapper.toCardResponseList(cardPage.getContent());
        PageResponse<CardResponse> response = PageResponse.fromPage(cardPage, cardResponseList);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Создать новую карту (Админ)",
            description = "Создает новую карту для указанного пользователя.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Карта успешно создана",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CardResponse.class))),
    })
    @PostMapping("/admin/cards")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CardResponse> createCard(@Valid @RequestBody CreateCardRequest createCardRequest) {
        Card newCard = cardService.createCard(createCardRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(cardMapper.toCardResponse(newCard));
    }


    @Operation(summary = "Получить детали любой карты (Админ)",
            description = "Возвращает детали карты по ID.")
    @ApiResponses()
    @GetMapping("/admin/cards/{cardId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CardResponse> getCardById_Admin(@PathVariable Long cardId) {
        Card card = cardService.findCardById_Admin(cardId);
        return ResponseEntity.ok(cardMapper.toCardResponse(card));
    }

    @Operation(summary = "Изменить статус карты (Админ)",
            description = "Активирует или блокирует карту по ID.")
    @ApiResponses()
    @PutMapping("/admin/cards/{cardId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CardResponse> updateCardStatus_Admin(
            @PathVariable Long cardId,
            @Valid @RequestBody UpdateCardStatusRequest updateRequest) {
        Card updatedCard = cardService.updateCardStatus_Admin(cardId, updateRequest.getStatus());
        return ResponseEntity.ok(cardMapper.toCardResponse(updatedCard));
    }

    @Operation(summary = "Удалить карту (Админ)", description = "Удаляет карту из системы по ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Карта успешно удалена"),
    })
    @DeleteMapping("/admin/cards/{cardId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCard_Admin(@PathVariable Long cardId) {
        cardService.deleteCard_Admin(cardId);
        return ResponseEntity.noContent().build();
    }

}
