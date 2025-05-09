package com.example.bankcardmanagement.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.Set;

@Data
public class UpdateUserRolesRequest {
    @NotEmpty(message = "Список ролей не может быть пустым.")
    @Size(min = 1, message = "Должна быть указана хотя бы одна роль.")
    private Set<String> roles;
}
