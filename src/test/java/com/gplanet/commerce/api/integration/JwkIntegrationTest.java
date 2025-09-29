package com.gplanet.commerce.api.integration;

import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_JWKS;
import static com.gplanet.commerce.api.testutils.TestConstants.ENDPOINT_OPENID_CONFIG;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JwkIntegrationTest extends AbstractIntegrationTest {

    @Test
    void jwksEndpointReturnsValidKeys() throws Exception {
        mockMvc.perform(get(ENDPOINT_JWKS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys").isArray())
                .andExpect(jsonPath("$.keys[0].kty").value("RSA"))
                .andExpect(jsonPath("$.keys[0].use").value("sig"))
                .andExpect(jsonPath("$.keys[0].kid").exists())
                .andExpect(jsonPath("$.keys[0].alg").value("RS256"))
                .andExpect(jsonPath("$.keys[0].n").exists())
                .andExpect(jsonPath("$.keys[0].e").exists());
    }

    @Test
    void openidConfigurationEndpointReturnsValidConfig() throws Exception {
        mockMvc.perform(get(ENDPOINT_OPENID_CONFIG))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issuer").exists())
                .andExpect(jsonPath("$.jwks_uri").exists())
                .andExpect(jsonPath("$.token_endpoint").exists())
                .andExpect(jsonPath("$.grant_types_supported").isArray())
                .andExpect(jsonPath("$.id_token_signing_alg_values_supported").isArray());
    }
}
