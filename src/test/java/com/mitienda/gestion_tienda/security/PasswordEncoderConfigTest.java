package com.mitienda.gestion_tienda.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class PasswordEncoderConfigTest {
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void whenEncodingPassword_thenGeneratesDifferentHash() {
        // Arrange
        String rawPassword = "testPassword";

        // Act
        String encodedPassword1 = passwordEncoder.encode(rawPassword);
        String encodedPassword2 = passwordEncoder.encode(rawPassword);

        // Assert
        assertNotEquals(encodedPassword1, encodedPassword2);
        assertTrue(passwordEncoder.matches(rawPassword, encodedPassword1));
        assertTrue(passwordEncoder.matches(rawPassword, encodedPassword2));
    }

    @Test
    void whenMatchingPassword_thenSucceeds() {
        // Arrange
        String rawPassword = "testPassword";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // Act & Assert
        assertTrue(passwordEncoder.matches(rawPassword, encodedPassword));
    }

    @Test
    void whenMatchingWrongPassword_thenFails() {
        // Arrange
        String rawPassword = "testPassword";
        String wrongPassword = "wrongPassword";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // Act & Assert
        assertFalse(passwordEncoder.matches(wrongPassword, encodedPassword));
    }
}
