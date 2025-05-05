package com.example.bankcardmanagement.service;

import com.example.bankcardmanagement.dto.request.CreateUserRequest;
import com.example.bankcardmanagement.dto.request.LoginRequest;
import com.example.bankcardmanagement.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse login(LoginRequest loginRequest);

    AuthResponse register(CreateUserRequest createUserRequest);
}