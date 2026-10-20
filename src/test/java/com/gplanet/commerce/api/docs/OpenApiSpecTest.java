package com.gplanet.commerce.api.docs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.Separators;
import com.fasterxml.jackson.databind.JsonNode;
import com.gplanet.commerce.api.integration.AbstractIntegrationTest;

/**
 * Keeps the published OpenAPI document in sync with the code.
 *
 * <p>The docs site renders {@code docs-site/public/openapi.json}. This test generates the document and
 * fails if the committed file differs, so every API change shows up as a diff in its pull request.
 * Regenerate the file with {@code ./mvnw test -Dtest=OpenApiSpecTest -Dopenapi.update=true}.</p>
 */
class OpenApiSpecTest extends AbstractIntegrationTest {

    private static final Path SPEC_FILE = Path.of("docs-site", "public", "openapi.json");
    private static final String UPDATE_COMMAND = "./mvnw test -Dtest=OpenApiSpecTest -Dopenapi.update=true";

    /**
     * The test profile raises the rate limits so tests don't hit them, but the document describes the
     * production limits, so they are read from the main application.yml.
     */
    @DynamicPropertySource
    static void productionSettings(DynamicPropertyRegistry registry) {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yml"));
        Properties properties = yaml.getObject();
        // Not stringPropertyNames(): it skips the numbers, which YAML loads as integers
        properties.forEach((key, value) -> {
            String name = key.toString();
            if (name.startsWith("rate-limit.") || name.startsWith("jwt.")) {
                registry.add(name, () -> value);
            }
        });
    }

    @Test
    void publishedSpecMatchesTheCode() throws Exception {
        String generated = generateSpec();

        if (Boolean.getBoolean("openapi.update")) {
            Files.createDirectories(SPEC_FILE.getParent());
            Files.writeString(SPEC_FILE, generated, StandardCharsets.UTF_8);
            return;
        }

        assertTrue(Files.exists(SPEC_FILE), SPEC_FILE + " is missing. Generate it with: " + UPDATE_COMMAND);
        String published = Files.readString(SPEC_FILE, StandardCharsets.UTF_8);
        if (!published.equals(generated)) {
            fail(SPEC_FILE + " is out of date. Regenerate it with " + UPDATE_COMMAND + " and commit it. "
                + firstDifference(published, generated));
        }
    }

    @Test
    void specDescribesTheProductionDeployment() throws Exception {
        JsonNode spec = objectMapper.readTree(generateSpec());

        assertEquals("https://g-commerce.duckdns.org", spec.at("/servers/0/url").asText());
        assertFalse(spec.at("/info/description").asText().contains("{{"), "Unresolved placeholder");
        assertTrue(spec.at("/info/description").asText().contains("| Anonymous | IP address | 10 | 5 |"),
            "The description should show the production rate limits");
        List<String> tags = new ArrayList<>();
        spec.get("tags").forEach(tag -> tags.add(tag.get("name").asText()));
        assertEquals(List.of("Authentication", "Users", "Products", "Purchases", "Discovery"), tags);
    }

    private String generateSpec() throws Exception {
        String json = mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return prettyPrint(json);
    }

    // Two-space indentation and one array item per line, like JSON.stringify(spec, null, 2), for readable diffs
    private String prettyPrint(String json) throws IOException {
        DefaultIndenter indenter = new DefaultIndenter("  ", "\n");
        DefaultPrettyPrinter printer = new DefaultPrettyPrinter()
            .withSeparators(Separators.createDefaultInstance()
                .withObjectFieldValueSpacing(Separators.Spacing.AFTER)
                .withArrayEmptySeparator("")
                .withObjectEmptySeparator(""));
        printer.indentObjectsWith(indenter);
        printer.indentArraysWith(indenter);
        return objectMapper.writer(printer).writeValueAsString(objectMapper.readTree(json)) + "\n";
    }

    private static String firstDifference(String published, String generated) {
        String[] publishedLines = published.split("\n", -1);
        String[] generatedLines = generated.split("\n", -1);
        int lines = Math.min(publishedLines.length, generatedLines.length);
        for (int i = 0; i < lines; i++) {
            if (!publishedLines[i].equals(generatedLines[i])) {
                return String.format("First difference at line %d:%n  published: %s%n  generated: %s",
                    i + 1, publishedLines[i].strip(), generatedLines[i].strip());
            }
        }
        return String.format("The published file has %d lines and the generated one %d.",
            publishedLines.length, generatedLines.length);
    }
}
