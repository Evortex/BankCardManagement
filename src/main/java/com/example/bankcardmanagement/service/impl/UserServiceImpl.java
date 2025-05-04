package com.example.bankcardmanagement.service.impl;

import com.example.bankcardmanagement.dto.request.CreateUserRequest;
import com.example.bankcardmanagement.entity.Role;
import com.example.bankcardmanagement.entity.User;
import com.example.bankcardmanagement.enums.UserRole;
import com.example.bankcardmanagement.exception.BadRequestException;
import com.example.bankcardmanagement.exception.ResourceNotFoundException;
import com.example.bankcardmanagement.exception.UserAlreadyExistsException;
import com.example.bankcardmanagement.repository.RoleRepository;
import com.example.bankcardmanagement.repository.UserRepository;
import com.example.bankcardmanagement.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmailWithRoles(email)
                .orElseThrow(() -> {
                    logger.warn("Пользователь не найден по email: {}", email);
                    return new UsernameNotFoundException("Пользователь с email '" + email + "' не найден.");
                });
    }

    @Override
    @Transactional
    public User createUser(CreateUserRequest request) {
        logger.info("Попытка создания пользователя с email: {}", request.getEmail());
        if (userRepository.existsByEmail(request.getEmail())) {
            logger.warn("Попытка создания пользователя с существующим email: {}", request.getEmail());
            throw new UserAlreadyExistsException("Пользователь с email '"
                    + request.getEmail() + "' уже существует.");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());

        Role userRole = roleRepository.findByName(UserRole.ROLE_USER)
                .orElseThrow(() -> {
                    logger.error("Критическая ошибка: Роль {} не найдена в базе данных.", UserRole.ROLE_USER);
                    return new IllegalStateException("Стандартная роль пользователя не найдена.");
                });
        user.getRoles().add(userRole);

        User savedUser = userRepository.save(user);
        logger.info("Пользователь успешно создан с ID: {}", savedUser.getId());
        return savedUser;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findUserById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> findAllUsers(Pageable pageable) {
        logger.debug("Запрос списка всех пользователей с пагинацией: {}", pageable);
        return userRepository.findAll(pageable);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        logger.info("Попытка удаления пользователя с ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Попытка удаления несуществующего пользователя с ID: {}", id);
                    return new ResourceNotFoundException("User", "id", id);
                });
        userRepository.delete(user);
        logger.info("Пользователь с ID: {} успешно удален", id);
    }

    @Override
    @Transactional(readOnly = true)
    public User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new ResourceNotFoundException("Аутентифицированный пользователь не найден.");
        }

        String username;
        Object principal = authentication.getPrincipal();

        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else if (principal instanceof String) {
            username = (String) principal;
        } else {
            throw new IllegalStateException("Неизвестный тип principal в SecurityContext: "
                    + principal.getClass());
        }


        return userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", username));
    }

    @Transactional
    @Override
    public User updateUserRoles(Long userId, Set<String> roleNames) {
        logger.info("Обновление ролей для пользователя ID: {} на {}", userId, roleNames);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Set<Role> newRoles = new HashSet<>();
        for (String roleName : roleNames) {
            try {
                UserRole userRoleEnum = UserRole.valueOf(roleName.toUpperCase());
                Role role = roleRepository.findByName(userRoleEnum)
                        .orElseThrow(() -> new BadRequestException("Роль '" + roleName + "' не найдена."));
                newRoles.add(role);
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Некорректное имя роли: " + roleName);
            }
        }

        user.setRoles(newRoles);
        User updatedUser = userRepository.save(user);
        logger.info("Роли для пользователя ID: {} успешно обновлены.", userId);
        return updatedUser;
    }
}
