package com.example.bankcardmanagement.mapper;

import com.example.bankcardmanagement.dto.response.UserResponse;
import com.example.bankcardmanagement.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UserMapper {

    public UserResponse toUserResponse(User user) {
        if (user == null) {
            return null;
        }
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());

        Set<String> roleNames = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        response.setRoles(roleNames);

        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());

        return response;
    }

    public List<UserResponse> toUserResponseList(List<User> users) {
        if (users == null) {
            return null;
        }
        return users.stream()
                .map(this::toUserResponse)
                .collect(Collectors.toList());
    }
}
