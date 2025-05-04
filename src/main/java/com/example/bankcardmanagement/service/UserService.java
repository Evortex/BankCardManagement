package com.example.bankcardmanagement.service;

import com.example.bankcardmanagement.dto.request.CreateUserRequest;
import com.example.bankcardmanagement.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

public interface UserService extends UserDetailsService {

    User createUser(CreateUserRequest createUserRequest);

    Optional<User> findUserById(Long id);

    Optional<User> findUserByEmail(String email);

    Page<User> findAllUsers(Pageable pageable);

    void deleteUser(Long id);

    User getCurrentAuthenticatedUser();

    @Transactional
    User updateUserRoles(Long userId, Set<String> roleNames);
}
