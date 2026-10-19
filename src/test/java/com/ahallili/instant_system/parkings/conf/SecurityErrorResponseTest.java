package com.ahallili.instant_system.parkings.conf;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class SecurityErrorResponseTest {
    @Autowired private WebApplicationContext context;
    @Autowired private FilterChainProxy securityFilterChain;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilterChain).build();
    }

    @Test
    void missingAndInvalidTokensHaveSameResponse() throws Exception {
        mvc.perform(get("/api/available-cities"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string(SecurityErrorHandler.UNAUTHORIZED_MESSAGE));
        mvc.perform(get("/api/available-cities").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(SecurityErrorHandler.UNAUTHORIZED_MESSAGE));
    }

    @Test
    void logoutWithoutTokenUsesSameResponse() throws Exception {
        mvc.perform(delete("/api/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(SecurityErrorHandler.UNAUTHORIZED_MESSAGE));
    }

    @Test
    void publicEndpointStillWorksWithoutToken() throws Exception {
        mvc.perform(get("/api/hello")).andExpect(status().isOk());
    }
}
