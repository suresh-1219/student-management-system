package com.suresh.sms.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.suresh.sms.support.AbstractIntegrationTest;

/**
 * Starts the whole application with the "prod" profile added (on top of
 * "test", which supplies the database and JWT secret), so a typo in
 * application-prod.properties fails here instead of on a server.
 *
 * SWAGGER_ENABLED is pinned so a developer who happens to have it set in
 * their own environment cannot make this test fail.
 */
@SpringBootTest(properties = "SWAGGER_ENABLED=false")
@AutoConfigureMockMvc
@ActiveProfiles("prod")
class ProdProfileTest extends AbstractIntegrationTest {

    @Autowired
    private Environment environment;

    @Autowired
    private MockMvc mockMvc;


    @Test
    void prodSettingsAreApplied() {

        assertEquals("false", environment.getProperty("spring.jpa.show-sql"));
        assertEquals("logstash", environment.getProperty("logging.structured.format.console"));
        assertEquals("graceful", environment.getProperty("server.shutdown"));
    }

    @Test
    void apiDocsAreHiddenUnlessSwaggerIsEnabled() throws Exception {

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isNotFound());
    }
}
