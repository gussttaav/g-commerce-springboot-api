package com.gplanet.commerce.api.services;

import javax.sql.DataSource;

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service class that restores the demo data of the public deployment: the demo accounts, the product catalog and
 * their purchases, as in {@value #DEMO_DATA_SCRIPT}.
 * <p>
 * Everything visitors created or changed is deleted, and every refresh token is revoked, so the sessions of the
 * deleted accounts end. The IDs of deleted rows are not reused (see the script), so the access tokens of deleted
 * accounts, valid until they expire, can't act as anyone else.
 * </p>
 *
 * @author Gustavo
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DemoDataService {

    /** The classpath script that deletes all the data and inserts the demo data. */
    public static final String DEMO_DATA_SCRIPT = "db/demo-data.sql";

    private final DataSource dataSource;
    private final TransactionTemplate transactionTemplate;
    private final AuthService authService;

    /**
     * Replaces all the users, products and purchases with the demo data, in one transaction, and then revokes
     * every refresh token.
     */
    public void resetDemoData() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(new ClassPathResource(DEMO_DATA_SCRIPT));
        // The populator takes the connection of the current transaction, so a failure leaves the data unchanged
        transactionTemplate.executeWithoutResult(status -> populator.execute(dataSource));
        long revoked = authService.revokeAllRefreshTokens();
        log.info("Demo data restored; {} refresh tokens revoked", revoked);
    }
}
