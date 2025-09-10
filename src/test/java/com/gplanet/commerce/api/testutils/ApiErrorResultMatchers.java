package com.gplanet.commerce.api.testutils;

import org.springframework.test.web.servlet.ResultMatcher;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * Utility class for asserting API error responses in tests.
 */
public final class ApiErrorResultMatchers {

    // JSON Paths
    public static final String JSON_ERROR = "$.error";
    public static final String JSON_DETAILS = "$.details";

    // Common error messages
    public static final String VALIDATION_ERROR = "Validation Error";
    public static final String AUTHORIZATION_ERROR = "Forbidden";
    public static final String NOT_FOUND_ERROR = "Not Found";

    private ApiErrorResultMatchers() {}

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
     * Assert that details contain a given substring (useful for field validation messages).
     */
    public static ResultMatcher detailContains(String expectedSubstring) throws Exception {
        return jsonPath(JSON_DETAILS + "[0]").value(containsString(expectedSubstring));
    }
}
