package com.gplanet.commerce.api.integration;

import static com.gplanet.commerce.api.testutils.ApiErrorResultMatchers.errorMessage;
import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_AUTH_REFRESH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;

import com.gplanet.commerce.api.dtos.auth.LoginDTO;
import com.gplanet.commerce.api.entities.Product;
import com.gplanet.commerce.api.entities.User;
import com.gplanet.commerce.api.services.DemoDataResetScheduler;
import com.gplanet.commerce.api.services.DemoDataService;

/**
 * The demo data reset restores the demo accounts, products and purchases and deletes everything else.
 * <p>
 * The tests only insert rows before a reset: unlike MySQL, H2 doesn't move its ID counters past the IDs that the
 * script inserts explicitly, so a later insert could get the ID of a demo row.
 * </p>
 */
class DemoDataResetIntegrationTest extends AbstractIntegrationTest {

    private static final long DEMO_USERS = 5;
    private static final long DEMO_PRODUCTS = 25;
    private static final long DEMO_PURCHASES = 8;
    private static final long MONITOR_ID = 3;

    /** A row of the demo accounts table of the API overview: | `email` | `password` | role | */
    private static final Pattern DEMO_ACCOUNT_ROW =
        Pattern.compile("^\\| `([^`]+@[^`]+)` \\| `([^`]+)` \\|", Pattern.MULTILINE);

    @Autowired
    private DemoDataService demoDataService;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void resetLoadsTheDemoDataWithTheDocumentedAccounts() throws IOException {
        demoDataService.resetDemoData();

        assertEquals(DEMO_USERS, userRepository.count());
        assertEquals(DEMO_PRODUCTS, productRepository.count());
        assertEquals(DEMO_PURCHASES, purchaseRepository.count());

        User admin = userRepository.findById(1L).orElseThrow();
        assertEquals("admin@example.com", admin.getEmail());
        assertEquals(User.Role.ADMIN, admin.getRole());
        User john = userRepository.findById(2L).orElseThrow();
        assertEquals("john@example.com", john.getEmail());
        assertEquals(User.Role.USER, john.getRole());

        // The sign-in details published in the API overview work with the demo data
        Map<String, String> documentedAccounts = documentedDemoAccounts();
        assertEquals(Set.of(admin.getEmail(), john.getEmail()), documentedAccounts.keySet());
        documentedAccounts.forEach((email, secret) -> assertTrue(
            passwordEncoder.matches(secret, userRepository.findByEmail(email).orElseThrow().getPassword()), email));

        Product monitor = productRepository.findById(MONITOR_ID).orElseThrow();
        assertEquals("4K Monitor", monitor.getName());
    }

    @Test
    void resetDeletesWhatVisitorsCreatedAndEndsTheirSessions() throws Exception {
        createUser("Visitor", "visitor@example.com", "visitor-password", User.Role.USER);
        String refreshToken = authService.login(new LoginDTO("visitor@example.com", "visitor-password"))
            .refreshToken();

        demoDataService.resetDemoData();

        assertFalse(userRepository.existsByEmail("visitor@example.com"));
        assertEquals(DEMO_USERS, userRepository.count());
        mockMvc.perform(post(ENDPOINT_AUTH_REFRESH)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + refreshToken))
                .andExpect(status().isUnauthorized())
                .andExpect(errorMessage("Refresh token has already been used or revoked"));
    }

    @Test
    void resetUndoesChangesToTheDemoData() {
        demoDataService.resetDemoData();
        User john = userRepository.findById(2L).orElseThrow();
        john.setName("Changed by a visitor");
        john.setRole(User.Role.ADMIN);
        userRepository.save(john);
        Product monitor = productRepository.findById(MONITOR_ID).orElseThrow();
        monitor.setPrice(new BigDecimal("1.00"));
        monitor.setActive(false);
        productRepository.save(monitor);

        demoDataService.resetDemoData();

        john = userRepository.findById(2L).orElseThrow();
        assertEquals("John Doe", john.getName());
        assertEquals(User.Role.USER, john.getRole());
        monitor = productRepository.findById(MONITOR_ID).orElseThrow();
        assertEquals(0, new BigDecimal("399.99").compareTo(monitor.getPrice()));
        assertTrue(monitor.isActive());
    }

    @Test
    void resetIsNotScheduledByDefault() {
        assertTrue(applicationContext.getBeansOfType(DemoDataResetScheduler.class).isEmpty());
    }

    private static Map<String, String> documentedDemoAccounts() throws IOException {
        String overview = new ClassPathResource("openapi/description.md").getContentAsString(StandardCharsets.UTF_8);
        Map<String, String> accounts = new HashMap<>();
        Matcher row = DEMO_ACCOUNT_ROW.matcher(overview);
        while (row.find()) {
            accounts.put(row.group(1), row.group(2));
        }
        return accounts;
    }
}
