package com.example.bankcardmanagement.dto.request;

import com.example.bankcardmanagement.enums.CardStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateCardStatusRequest {
    @NotNull(message = "Новый статус карты не может быть пустым")
    private CardStatus status;
}
