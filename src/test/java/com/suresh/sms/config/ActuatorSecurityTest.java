package com.suresh.sms.config;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import com.suresh.sms.support.AbstractIntegrationTest;

/**
 * Spring Boot switches metrics export off inside tests by default, which
 * would make /actuator/prometheus not exist at all.
 * {@code @AutoConfigureObservability} turns it back on for this class.
 */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureObservability
class ActuatorSecurityTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;


    @Test
    void infoIsPublic() throws Exception {

        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk());
    }

    @Test
    void metricsRequireLogin() throws Exception {

        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void metricsAreForbiddenForANormalUser() throws Exception {

        mockMvc.perform(
                get("/actuator/prometheus")
                        .with(user("plainuser").roles("USER"))
        )
        .andExpect(status().isForbidden());
    }

    @Test
    void metricsAreAvailableToAdminsIncludingBusinessGauges() throws Exception {

        mockMvc.perform(
                get("/actuator/prometheus")
                        .with(user("boss").roles("ADMIN"))
        )
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("jvm_memory_used_bytes")))
        .andExpect(content().string(containsString("sms_students")))
        .andExpect(content().string(containsString("sms_courses")))
        .andExpect(content().string(containsString("sms_enrollments")));
    }
}
