package com.example.bankcardmanagement.security.jwt;

import com.example.bankcardmanagement.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);
    private static final String AUTHORITIES_KEY = "roles";

    @Value("${app.jwt.secret}")
    private String jwtSecretString;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationInMs;

    private SecretKey key;

    @PostConstruct
    public void init() {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(jwtSecretString);
            this.key = Keys.hmacShaKeyFor(keyBytes);
            logger.info("JWT Secret Key успешно инициализирован.");
        } catch (IllegalArgumentException e) {
            logger.error("Ошибка декодирования JWT Secret Key из Base64. Убедитесь, что ключ корректен.", e);
            throw new IllegalStateException("Некорректный JWT Secret Key в конфигурации", e);
        }
    }

    public String generateToken(Authentication authentication) {
        User userPrincipal = (User) authentication.getPrincipal();
        return generateToken(userPrincipal);
    }

    public String generateToken(User user) {
        return buildToken(user.getEmail(), user.getId(), user.getAuthorities());
    }

    private String buildToken(String email, Long userId, Collection<? extends GrantedAuthority> authorities) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        String authoritiesStr = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        return Jwts.builder()
                .setSubject(email)
                .claim(AUTHORITIES_KEY, authoritiesStr)
                .claim("userId", userId)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }


    public String getUserEmailFromJWT(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(authToken);
            return true;
        } catch (SignatureException ex) {
            logger.error("Неверная подпись JWT: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            logger.error("Некорректный формат JWT: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            logger.error("Срок действия JWT истек: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            logger.error("Неподдерживаемый JWT: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            logger.error("JWT claims строка пуста или ключ невалиден: {}", ex.getMessage());
        }
        return false;
    }
}
