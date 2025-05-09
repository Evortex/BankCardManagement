package com.example.bankcardmanagement.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Set;

@Data
public class UserResponse {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private Set<String> roles;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
