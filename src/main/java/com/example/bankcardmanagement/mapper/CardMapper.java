package com.example.bankcardmanagement.mapper;


import com.example.bankcardmanagement.dto.response.CardResponse;
import com.example.bankcardmanagement.entity.Card;
import com.example.bankcardmanagement.service.EncryptionService;
import com.example.bankcardmanagement.util.CardMaskingUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CardMapper {

    private final EncryptionService encryptionService;

    public CardResponse toCardResponse(Card card) {
        if (card == null) {
            return null;
        }
        CardResponse response = new CardResponse();
        response.setId(card.getId());

        String decryptedCardNumber = encryptionService.decrypt(card.getCardNumberEncrypted());
        response.setMaskedCardNumber(CardMaskingUtil.maskCardNumber(decryptedCardNumber));

        response.setCardHolderName(card.getCardHolderName());
        response.setExpiryDate(card.getExpiryDate());
        response.setStatus(card.getStatus());
        response.setBalance(card.getBalance());
        response.setOwnerId(card.getOwner().getId());
        response.setCreatedAt(card.getCreatedAt());
        response.setUpdatedAt(card.getUpdatedAt());

        return response;
    }

    public List<CardResponse> toCardResponseList(List<Card> cards) {
        if (cards == null) {
            return null;
        }
        return cards.stream()
                .map(this::toCardResponse)
                .collect(Collectors.toList());
    }
}
