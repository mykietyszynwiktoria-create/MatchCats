package pl.viksi.catsmatch.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class BillingApiTests {
    @Autowired MockMvc mvc;

    @Test void publicPlanCatalogIsAvailable() throws Exception {
        mvc.perform(get("/billing/plans"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value("FREE"))
            .andExpect(jsonPath("$[0].available").value(true))
            .andExpect(jsonPath("$[1].id").value("PREMIUM"))
            .andExpect(jsonPath("$[1].available").value(false));
    }

    @Test void accountPlanDefaultsToFreeAndCheckoutStaysDisabled() throws Exception {
        mvc.perform(post("/users").with(csrf()).contentType("application/json")
                .content("{\"username\":\"billing-user\",\"password\":\"StrongTestPassword!\",\"email\":\"billing@example.test\",\"firstName\":\"Billing\",\"surname\":\"Test\"}"))
            .andExpect(status().isCreated());
        mvc.perform(get("/billing/me").with(user("billing-user")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.planId").value("FREE"))
            .andExpect(jsonPath("$.active").value(true));
        mvc.perform(post("/billing/checkout").with(user("billing-user")).with(csrf()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("PAYMENT_NOT_CONFIGURED"));
    }
}

