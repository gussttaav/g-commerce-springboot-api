package com.gplanet.commerce.api.configs.security;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.logging.Log;
import org.springframework.boot.DefaultPropertiesPropertySource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

/**
 * Chooses the {@code https} or {@code http} profile for an application started without an explicit profile.
 * <p>
 * If a keystore exists, {@code https} becomes the default profile and the keystore becomes the default key store of
 * that profile ({@code SSL_KEY_STORE}); otherwise the default profile is {@code http}. The keystore is looked up in
 * the {@code /certs} directory, where a Docker container mounts it, and then on the classpath, where it can be
 * placed in development.
 * </p>
 * <p>
 * Default profiles only apply when no profile is active, so a profile set explicitly, e.g. with
 * {@code SPRING_PROFILES_ACTIVE}, always wins. This runs before the configuration files are loaded, so that the
 * documents of the chosen profile are loaded too.
 * </p>
 *
 * @author Gustavo
 * @version 1.0
 */
public class SslProfileDetector implements EnvironmentPostProcessor, Ordered {

    /** Runs right before the configuration files are loaded. */
    public static final int ORDER = ConfigDataEnvironmentPostProcessor.ORDER - 1;

    static final String HTTPS_PROFILE = "https";
    static final String HTTP_PROFILE = "http";
    static final String KEY_STORE_PROPERTY = "SSL_KEY_STORE";

    private static final String DEFAULT_PROFILES_PROPERTY = "spring.profiles.default";
    private static final List<String> KEYSTORE_LOCATIONS = List.of(
            "file:/certs/keystore.p12",
            "classpath:keystore.p12");

    private final Log log;
    private final List<String> keystoreLocations;

    /**
     * Creates the detector that looks for the keystore in its standard locations.
     *
     * @param logFactory the factory of loggers whose output is deferred until logging is initialized
     */
    public SslProfileDetector(DeferredLogFactory logFactory) {
        this(logFactory, KEYSTORE_LOCATIONS);
    }

    /**
     * Creates a detector that looks for the keystore in the given locations.
     *
     * @param logFactory the factory of loggers whose output is deferred until logging is initialized
     * @param keystoreLocations the resource locations of the keystore, in order of preference
     */
    SslProfileDetector(DeferredLogFactory logFactory, List<String> keystoreLocations) {
        this.log = logFactory.getLog(SslProfileDetector.class);
        this.keystoreLocations = List.copyOf(keystoreLocations);
    }

    /**
     * Sets the default profile, and the default key store when a keystore is found.
     *
     * @param environment the Spring configurable environment
     * @param application the Spring Boot application
     */
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        ResourceLoader resourceLoader = Optional.ofNullable(application.getResourceLoader())
                .orElseGet(() -> new DefaultResourceLoader(application.getClassLoader()));
        Optional<String> keystore = keystoreLocations.stream()
                .filter(location -> resourceLoader.getResource(location).exists())
                .findFirst();

        Map<String, Object> defaults = new HashMap<>();
        if (keystore.isPresent()) {
            log.info("Keystore found at " + keystore.get() + ": the default profile is " + HTTPS_PROFILE);
            defaults.put(DEFAULT_PROFILES_PROPERTY, HTTPS_PROFILE);
            defaults.put(KEY_STORE_PROPERTY, keystore.get());
        } else {
            log.info("No keystore found: the default profile is " + HTTP_PROFILE);
            defaults.put(DEFAULT_PROFILES_PROPERTY, HTTP_PROFILE);
        }
        // Lowest precedence: any explicit setting of these properties wins
        DefaultPropertiesPropertySource.addOrMerge(defaults, environment.getPropertySources());
    }

    /**
     * Returns the order of this post-processor, which runs before the configuration files are loaded.
     *
     * @return int the order
     */
    @Override
    public int getOrder() {
        return ORDER;
    }
}
