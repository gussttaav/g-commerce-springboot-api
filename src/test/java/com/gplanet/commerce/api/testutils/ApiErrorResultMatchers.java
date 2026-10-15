package com.gplanet.commerce.api.testutils;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import org.springframework.test.web.servlet.ResultMatcher;

/**
 * Utility class that provides custom ResultMatchers for testing API error responses.
 * This class works in conjunction with {@link com.gplanet.commerce.api.dtos.api.ApiErrorDTO}
 * and {@link com.gplanet.commerce.api.exceptions.GlobalExceptionHandler} to verify error
 * responses in integration tests.
 * 
 * <p>The matchers correspond to the standard error response structure:
 * <pre>
 * {
 *   "timestamp": "2023-10-01T10:00:00",
 *   "status": 400,
 *   "error": "Validation Error",
 *   "message": "Error description",
 *   "path": "/api/endpoint",
 *   "details": ["Error detail 1", "Error detail 2"]
 * }
 * </pre>
 * </p>
 * 
 * <p>Usage example:
 * <pre>
 * mockMvc.perform(post("/api/users"))
 *        .andExpect(status().isBadRequest())
 *        .andExpect(ApiErrorResultMatchers.validationError())
 *        .andExpect(ApiErrorResultMatchers.errorDetailsIsNotEmpty());
 * </pre>
 * </p>
 * 
 * @author Gustavo
 * @version 1.0
 */
public final class ApiErrorResultMatchers {

    // JSON Paths
    public static final String JSON_ERROR = "$.error";
    public static final String JSON_DETAILS = "$.details";
    public static final String JSON_MSG = "$.message";

    // Common error messages
    public static final String VALIDATION_ERROR = "Validation Error";
    public static final String AUTHORIZATION_ERROR = "Forbidden";
    public static final String NOT_FOUND_ERROR = "Not Found";

    private ApiErrorResultMatchers() {   
    }

    /**
     * Assert that the error type equals "Validation Error".
     */
    public static ResultMatcher validationError() throws Exception {
        return jsonPath(JSON_ERROR).value(VALIDATION_ERROR);
    }

    /**
     * Assert that the error type equals "Forbidden".
     */
    public static ResultMatcher forbiddenError() throws Exception {
        return jsonPath(JSON_ERROR).value(AUTHORIZATION_ERROR);
    }

    /**
     * Assert that the error type equals "Not Found".
     */
    public static ResultMatcher notFoundError() throws Exception {
        return jsonPath(JSON_ERROR).value(NOT_FOUND_ERROR);
    }

    /**
     * Assert that details is an Array.
     */
    public static ResultMatcher errorDetailsAsArray() throws Exception {
        return jsonPath(JSON_DETAILS).isArray();
    }

    /**
     * Assert that details is an Array.
     */
    public static ResultMatcher errorDetailsIsNotEmpty() throws Exception {
        return jsonPath(JSON_DETAILS, hasSize(greaterThan(0)));
    }

    /**
     * Assert that details contain a given substring (useful for field validation messages).
     */
    public static ResultMatcher errorDetailContains(String expectedSubstring) throws Exception {
        return jsonPath(JSON_DETAILS + "[0]").value(containsString(expectedSubstring));
    }

    /**
     * Assert that error message is equal to the given message.
     */
    public static ResultMatcher errorMessage(String msg) throws Exception {
        return jsonPath(JSON_MSG).value(msg);
    }
}
