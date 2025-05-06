package com.example.bankcardmanagement.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import java.time.YearMonth;

@Data
public class CreateCardRequest {

    @NotNull(message = "Необходимо указать ID владельца карты")
    private Long ownerId;

    @NotBlank(message = "Номер карты не может быть пустым")
    @Pattern(regexp = "^\\d{16}$", message = "Номер карты должен состоять из 16 цифр")
    private String cardNumber;

    @NotNull(message = "Срок действия не может быть пустым")
    @Future(message = "Срок действия должен быть в будущем")
    private YearMonth expiryDate;
}
