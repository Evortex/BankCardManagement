package com.example.bankcardmanagement.service.impl;

import com.example.bankcardmanagement.dto.request.CreateUserRequest;
import com.example.bankcardmanagement.dto.request.LoginRequest;
import com.example.bankcardmanagement.dto.response.AuthResponse;
import com.example.bankcardmanagement.entity.User;
import com.example.bankcardmanagement.exception.BadRequestException;
import com.example.bankcardmanagement.exception.UserAlreadyExistsException;
import com.example.bankcardmanagement.security.jwt.JwtTokenProvider;
import com.example.bankcardmanagement.service.AuthService;
import com.example.bankcardmanagement.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    @Override
    public AuthResponse login(LoginRequest loginRequest) {
        logger.info("Попытка аутентификации пользователя: {}", loginRequest.getEmail());
        try {
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword());

            Authentication authentication = authenticationManager.authenticate(authenticationToken);

            SecurityContextHolder.getContext().setAuthentication(authentication);

            String jwt = tokenProvider.generateToken(authentication);
            logger.info("Пользователь {} успешно аутентифицирован.", loginRequest.getEmail());

            return new AuthResponse(jwt);

        } catch (Exception e) {
            logger.error("Ошибка аутентификации для пользователя {}: {}", loginRequest.getEmail(), e.getMessage());
            throw new BadRequestException("Неверный email или пароль.");
        }
    }

    @Override
    @Transactional
    public AuthResponse register(CreateUserRequest createUserRequest) {
        logger.info("Попытка регистрации нового пользователя: {}", createUserRequest.getEmail());
        try {
            User newUser = userService.createUser(createUserRequest);
            logger.info("Пользователь {} успешно зарегистрирован с ID: {}", newUser.getEmail(), newUser.getId());

            String jwt = tokenProvider.generateToken(newUser);

            logger.info("JWT токен сгенерирован для нового пользователя {}", newUser.getEmail());
            return new AuthResponse(jwt);

        } catch (UserAlreadyExistsException e) {
            logger.warn("Попытка регистрации с существующим email: {}", createUserRequest.getEmail());
            throw e;
        } catch (Exception e) {
            logger.error("Ошибка во время регистрации пользователя {}: {}", createUserRequest.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Произошла ошибка во время регистрации.", e);
        }
    }
}
