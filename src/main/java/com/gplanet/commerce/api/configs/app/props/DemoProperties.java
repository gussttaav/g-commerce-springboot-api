package com.gplanet.commerce.api.configs.app.props;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * Configuration properties of the public demo: the shared demo accounts and the daily reset of the demo data.
 * Both are off by default, so outside the demo deployment the API behaves as usual.
 *
 * @author Gustavo
 * @version 1.0
 */
@ConfigurationProperties(prefix = "demo")
@Component
@Getter
@Setter
public class DemoProperties {

    /**
     * Emails of the shared demo accounts, whose email, password and role can't be changed, so no visitor can lock
     * the others out of them. Empty by default.
     */
    private List<String> protectedAccounts = List.of();

    /**
     * The daily reset of the demo data.
     */
    private Reset reset = new Reset();

    /**
     * Whether the account with this email is one of the protected demo accounts. Emails are compared ignoring case.
     *
     * @param email the email of the account
     * @return true if the account is protected
     */
    public boolean isProtectedAccount(String email) {
        return email != null && protectedAccounts.stream().anyMatch(email::equalsIgnoreCase);
    }

    /**
     * Configuration of the scheduled reset of the demo data.
     */
    @Getter
    @Setter
    public static class Reset {

        /**
         * Whether the demo data is reset on the {@code cron} schedule. It deletes all the data, so it is disabled by
         * default.
         */
        private boolean enabled;

        /**
         * When the demo data is reset, as a Spring cron expression in UTC. Every day at 03:00 by default.
         */
        private String cron = "0 0 3 * * *";
    }
}
