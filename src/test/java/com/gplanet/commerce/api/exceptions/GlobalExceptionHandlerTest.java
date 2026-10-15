package com.gplanet.commerce.api.exceptions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.SQLException;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gplanet.commerce.api.dtos.api.ApiErrorDTO;
import com.gplanet.commerce.api.dtos.auth.LoginDTO;
import com.gplanet.commerce.api.dtos.auth.LogoutRequestDTO;
import com.gplanet.commerce.api.dtos.purchase.PurchaseDTO;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;

/**
 * Tests for the error responses of {@link GlobalExceptionHandler}, which must be in English (OPS-2).
 */
class GlobalExceptionHandlerTest {

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new FailingController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void invalidRequestBodyReturnsValidationFailed() throws Exception {
        mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details", hasItems("email: Email is required", "password: Password is required")));
    }

    @Test
    void logoutWithoutRefreshTokenReturnsEnglishDetail() throws Exception {
        mockMvc.perform(post("/logout").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details", hasItem("refreshToken: Refresh token is required")));
    }

    @Test
    void purchaseWithoutProductsReturnsEnglishDetail() throws Exception {
        mockMvc.perform(post("/purchases").contentType(MediaType.APPLICATION_JSON).content("{\"products\": []}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details", hasItem("products: The purchase must have at least one product")));
    }

    @Test
    void constraintViolationReturnsValidationFailed() {
        ApiErrorDTO error = new GlobalExceptionHandler()
                .handleConstraintViolation(new ConstraintViolationException(Set.of()), new MockHttpServletRequest())
                .getBody();

        assertThat(error).isNotNull();
        assertThat(error.message()).isEqualTo("Validation failed");
    }

    @Test
    void malformedJsonReturnsEnglishMessage() throws Exception {
        mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed JSON request"));
    }

    @Test
    void parameterOfTheWrongTypeReturnsEnglishMessage() throws Exception {
        mockMvc.perform(get("/typed").param("page", "first"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parameter 'page' must be of type int"))
                .andExpect(jsonPath("$.details", hasItems("Received value: 'first'", "Expected type: int")));
    }

    @Test
    void missingAuthorizationHeaderReturnsEnglishMessage() throws Exception {
        mockMvc.perform(get("/header"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authorization header is required"))
                .andExpect(jsonPath("$.details", hasItem("The Authorization header with a Bearer token is missing")));
    }

    @Test
    void unexpectedErrorReturnsEnglishMessage() throws Exception {
        mockMvc.perform(get("/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }

    @Test
    void duplicateEntryReturnsEnglishMessage() throws Exception {
        mockMvc.perform(get("/integrity").param("cause", "Duplicate entry 'john@example.com' for key 'email'"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A record with this data already exists"))
                .andExpect(jsonPath("$.details", hasItem("Duplicate value: john@example.com")));
    }

    @Test
    void missingRequiredColumnReturnsEnglishMessage() throws Exception {
        mockMvc.perform(get("/integrity").param("cause", "Column 'email' cannot be null"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Required data is missing"))
                .andExpect(jsonPath("$.details", hasItem("Required field: email")));
    }

    @Test
    void foreignKeyViolationReturnsEnglishMessage() throws Exception {
        mockMvc.perform(get("/integrity").param("cause", "Cannot delete: a foreign key constraint fails"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Referential integrity violation"))
                .andExpect(jsonPath("$.details", hasItem("The operation would affect related data")));
    }

    @Test
    void otherIntegrityViolationReturnsEnglishMessage() throws Exception {
        mockMvc.perform(get("/integrity").param("cause", "Check constraint violated"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Data integrity violation"));
    }

    /**
     * Endpoints that fail in the ways the handler deals with.
     */
    @RestController
    static class FailingController {

        @PostMapping("/login")
        void login(@Valid @RequestBody LoginDTO request) {
            // Only validated
        }

        @PostMapping("/logout")
        void logout(@Valid @RequestBody LogoutRequestDTO request) {
            // Only validated
        }

        @PostMapping("/purchases")
        void purchase(@Valid @RequestBody PurchaseDTO request) {
            // Only validated
        }

        @GetMapping("/typed")
        void typed(@RequestParam int page) {
            // Only converted
        }

        @GetMapping("/header")
        void header(@RequestHeader("Authorization") String authorization) {
            // Only read
        }

        @GetMapping("/unexpected")
        void unexpected() {
            throw new IllegalStateException("Unexpected failure");
        }

        @GetMapping("/integrity")
        void integrity(@RequestParam String cause) {
            throw new DataIntegrityViolationException("Integrity violation", new SQLException(cause));
        }
    }
}
