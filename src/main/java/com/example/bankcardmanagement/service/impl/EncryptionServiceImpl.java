package com.example.bankcardmanagement.service.impl;

import com.example.bankcardmanagement.service.EncryptionService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;

@Service
public class EncryptionServiceImpl implements EncryptionService {

    private static final Logger logger = LoggerFactory.getLogger(EncryptionServiceImpl.class);
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";

    @Value("${app.encryption.key}")
    private String secretKeyString;

    private Cipher encryptCipher;
    private Cipher decryptCipher;

    @PostConstruct
    public void init() {
        try {
            byte[] decodedKey = Base64.getDecoder().decode(secretKeyString);
            if (decodedKey.length != 16 && decodedKey.length != 24 && decodedKey.length != 32) {
                throw new IllegalArgumentException("Неверная длина ключа AES: " + decodedKey.length * 8 + " бит. Должно быть 128, 192 или 256.");
            }
            SecretKey secretKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, ALGORITHM);

            encryptCipher = Cipher.getInstance(TRANSFORMATION);
            encryptCipher.init(Cipher.ENCRYPT_MODE, secretKey);

            decryptCipher = Cipher.getInstance(TRANSFORMATION);
            decryptCipher.init(Cipher.DECRYPT_MODE, secretKey);

            logger.info("EncryptionService инициализирован с трансформацией {}", TRANSFORMATION);

        } catch (GeneralSecurityException | IllegalArgumentException e) {
            logger.error("Ошибка инициализации EncryptionService", e);
            throw new IllegalStateException("Не удалось инициализировать EncryptionService", e);
        }
    }

    @Override
    public String encrypt(String data) {
        if (data == null) return null;
        try {
            byte[] encryptedBytes = encryptCipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (GeneralSecurityException e) {
            logger.error("Ошибка шифрования данных", e);
            throw new RuntimeException("Ошибка шифрования", e);
        }
    }

    @Override
    public String decrypt(String encryptedData) {
        if (encryptedData == null) return null;
        try {
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedData);
            byte[] decryptedBytes = decryptCipher.doFinal(encryptedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            logger.error("Ошибка дешифрования данных", e);
            throw new RuntimeException("Ошибка дешифрования", e);
        }
    }
}
