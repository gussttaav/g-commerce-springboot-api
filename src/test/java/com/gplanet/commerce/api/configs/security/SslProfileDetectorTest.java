package com.gplanet.commerce.api.configs.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Profiles;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

/**
 * Tests for {@link SslProfileDetector}: the profile set explicitly wins, and otherwise the {@code https} profile
 * is used when a keystore is found (OPS-1).
 */
class SslProfileDetectorTest {

    private static final String KEYSTORE = "keystore.p12";
    private static final String MISSING_KEYSTORE = "classpath:missing/keystore.p12";

    @TempDir
    private Path tempDir;

    @Test
    void keystoreMakesHttpsTheDefaultProfile() throws IOException {
        String keystore = existingKeystore();

        ConfigurableEnvironment environment = process(detector(MISSING_KEYSTORE, keystore));

        assertThat(environment.getActiveProfiles()).isEmpty();
        assertThat(environment.getDefaultProfiles()).containsExactly(SslProfileDetector.HTTPS_PROFILE);
        assertThat(environment.getProperty("server.port")).isEqualTo("8443");
        assertThat(environment.getProperty("server.ssl.key-store")).isEqualTo(keystore);
    }

    @Test
    void withoutKeystoreHttpIsTheDefaultProfile() {
        ConfigurableEnvironment environment = process(detector(MISSING_KEYSTORE));

        assertThat(environment.getActiveProfiles()).isEmpty();
        assertThat(environment.getDefaultProfiles()).containsExactly(SslProfileDetector.HTTP_PROFILE);
        assertThat(environment.getProperty("server.port")).isEqualTo("8080");
        assertThat(environment.containsProperty("server.ssl.key-store")).isFalse();
    }

    @Test
    void profileSetExplicitlyIsKeptWhenKeystoreIsFound() throws IOException {
        ConfigurableEnvironment environment = process(detector(existingKeystore()), "spring.profiles.active=http");

        assertThat(environment.getActiveProfiles()).containsExactly(SslProfileDetector.HTTP_PROFILE);
        assertThat(environment.acceptsProfiles(Profiles.of(SslProfileDetector.HTTPS_PROFILE))).isFalse();
        assertThat(environment.getProperty("server.port")).isEqualTo("8080");
        assertThat(environment.containsProperty("server.ssl.key-store")).isFalse();
    }

    @Test
    void keyStoreSetExplicitlyIsKept() throws IOException {
        String keyStore = "file:/elsewhere/keystore.p12";

        ConfigurableEnvironment environment = process(detector(existingKeystore()),
                SslProfileDetector.KEY_STORE_PROPERTY + "=" + keyStore);

        assertThat(environment.getProperty("server.ssl.key-store")).isEqualTo(keyStore);
    }

    @Test
    void runsBeforeTheConfigurationFilesAreLoaded() {
        assertThat(detector(MISSING_KEYSTORE).getOrder()).isLessThan(ConfigDataEnvironmentPostProcessor.ORDER);
    }

    @Test
    void applicationKeepsTheProfileSetExplicitly() {
        String profileArgument = "--spring.profiles.active=" + SslProfileDetector.HTTPS_PROFILE;
        try (ConfigurableApplicationContext context = run(new DefaultResourceLoader(), profileArgument)) {
            ConfigurableEnvironment environment = context.getEnvironment();

            assertThat(environment.getActiveProfiles()).containsExactly(SslProfileDetector.HTTPS_PROFILE);
            assertThat(environment.getProperty("server.port")).isEqualTo("8443");
        }
    }

    @Test
    void applicationUsesHttpsForAKeystorePackagedInTheJar() throws IOException {
        try (URLClassLoader classLoader = classLoaderWithKeystoreInJar();
                ConfigurableApplicationContext context = run(new DefaultResourceLoader(classLoader))) {
            ConfigurableEnvironment environment = context.getEnvironment();

            assertThat(environment.acceptsProfiles(Profiles.of(SslProfileDetector.HTTPS_PROFILE))).isTrue();
            assertThat(environment.getProperty("server.port")).isEqualTo("8443");
        }
    }

    private static SslProfileDetector detector(String... keystoreLocations) {
        return new SslProfileDetector(Supplier::get, List.of(keystoreLocations));
    }

    /**
     * Runs the detector and then loads the configuration files, as the application does on startup.
     */
    private static ConfigurableEnvironment process(SslProfileDetector detector, String... properties) {
        ConfigurableEnvironment environment = environmentWithoutVariables();
        TestPropertyValues.of(properties).applyTo(environment);
        detector.postProcessEnvironment(environment, new SpringApplication());
        ConfigDataEnvironmentPostProcessor.applyTo(environment);
        return environment;
    }

    private static ConfigurableApplicationContext run(ResourceLoader resourceLoader, String... args) {
        SpringApplication application = new SpringApplication(resourceLoader, EmptyConfiguration.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setBannerMode(Banner.Mode.OFF);
        application.setEnvironment(environmentWithoutVariables());
        return application.run(args);
    }

    /**
     * Creates an environment that ignores the environment variables, e.g. a SPRING_PROFILES_ACTIVE of the machine.
     */
    private static ConfigurableEnvironment environmentWithoutVariables() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        return environment;
    }

    private String existingKeystore() throws IOException {
        return Files.createFile(tempDir.resolve(KEYSTORE)).toUri().toString();
    }

    private URLClassLoader classLoaderWithKeystoreInJar() throws IOException {
        Path jar = tempDir.resolve("app.jar");
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar))) {
            out.putNextEntry(new JarEntry(KEYSTORE));
            out.write(new byte[] {0});
            out.closeEntry();
        }
        return new URLClassLoader(new URL[] {jar.toUri().toURL()}, SslProfileDetectorTest.class.getClassLoader());
    }

    @Configuration(proxyBeanMethods = false)
    static class EmptyConfiguration {
    }
}
