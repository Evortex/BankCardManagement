package com.example.bankcardmanagement.controller;

import com.example.bankcardmanagement.dto.request.UpdateUserRolesRequest;
import com.example.bankcardmanagement.dto.response.PageResponse;
import com.example.bankcardmanagement.dto.response.UserResponse;
import com.example.bankcardmanagement.entity.User;
import com.example.bankcardmanagement.mapper.UserMapper;
import com.example.bankcardmanagement.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Tag(name = "Управление пользователями (Админ)", description = "API для администраторов по управлению пользователями")
@SecurityRequirement(name = "bearerAuth")
public class AdminUserController {

    private final UserService userService;
    private final UserMapper userMapper;

    @Operation(summary = "Получить список всех пользователей (Админ)",
            description = "Возвращает пагинированный список всех пользователей в системе.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список пользователей получен",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Необходима аутентификация"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен (не администратор)")
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<UserResponse>> getAllUsers(
            @PageableDefault(sort = "id") Pageable pageable) {

        Page<User> userPage = userService.findAllUsers(pageable);
        List<UserResponse> userResponseList = userMapper.toUserResponseList(userPage.getContent());
        PageResponse<UserResponse> response = PageResponse.fromPage(userPage, userResponseList);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Получить пользователя по ID (Админ)",
            description = "Возвращает детали конкретного пользователя по его ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пользователь найден",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "401", description = "Необходима аутентификация"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    })
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long userId) {
        User user = userService.findUserById(userId)
                .orElseThrow(() -> new com.example.bankcardmanagement.exception.ResourceNotFoundException("User", "id",
                        userId));
        return ResponseEntity.ok(userMapper.toUserResponse(user));
    }

    @Operation(summary = "Обновить роли пользователя (Админ)",
            description = "Обновляет набор ролей для указанного пользователя.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Роли пользователя успешно обновлены",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400",
                    description = "Некорректные данные запроса (например, неверное имя роли)"),
            @ApiResponse(responseCode = "401", description = "Необходима аутентификация"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    })
    @PutMapping("/{userId}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateUserRoles(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRolesRequest request) {
        User updatedUser = userService.updateUserRoles(userId, request.getRoles());
        return ResponseEntity.ok(userMapper.toUserResponse(updatedUser));
    }

    @Operation(summary = "Удалить пользователя (Админ)",
            description = "Удаляет пользователя из системы по его ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Пользователь успешно удален"),
            @ApiResponse(responseCode = "401", description = "Необходима аутентификация"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    })
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}
