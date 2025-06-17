package com.gplanet.commerce_api.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

/**
 * Tests for password encoding functionality.
 * Verifies proper password hashing and matching behavior.
 */
@SpringBootTest
@ActiveProfiles("test")
class PasswordEncoderConfigTest {
    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Verify that encoding the same password twice produces different hashes.
     */
    @Test
    void shouldGenerateUniqueHashesForSamePassword() {
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

    /**
     * Verify that a password can be successfully matched with its hash.
     */
    @Test
    void shouldMatchValidPassword() {
        // Arrange
        String rawPassword = "testPassword";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // Act & Assert
        assertTrue(passwordEncoder.matches(rawPassword, encodedPassword));
    }

    /**
     * Verify that an incorrect password fails to match the hash.
     */
    @Test
    void shouldNotMatchInvalidPassword() {
        // Arrange
        String rawPassword = "testPassword";
        String wrongPassword = "wrongPassword";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // Act & Assert
        assertFalse(passwordEncoder.matches(wrongPassword, encodedPassword));
    }

    /**
     * Verify that empty passwords are handled appropriately.
     */
    @Test
    void shouldHandleEmptyPassword() {
        String emptyPassword = "";
        String encodedEmpty = passwordEncoder.encode(emptyPassword);
        
        assertNotNull(encodedEmpty);
        assertTrue(passwordEncoder.matches(emptyPassword, encodedEmpty));
    }

    /**
     * Verify that null passwords are handled appropriately.
     */
    @Test
    void shouldHandleNullPassword() {
        assertThrows(IllegalArgumentException.class, () -> {
            passwordEncoder.encode(null);
        });
    }
}
