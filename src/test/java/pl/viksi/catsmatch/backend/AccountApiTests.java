package pl.viksi.catsmatch.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.annotation.Transactional;
import pl.viksi.catsmatch.backend.account.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class AccountApiTests {
    @Autowired MockMvc mvc;
    @Autowired AccountRepository accounts;
    @Autowired ObjectMapper json;
    String body(String username) throws Exception {
        return json.writeValueAsString(Map.of("username",username,"password","StrongTestPassword!","email",username+"@example.test","firstName","Test","surname","Breeder"));
    }
    @Test void registrationHashesPasswordAndNeverReturnsIt() throws Exception {
        mvc.perform(post("/users").with(csrf()).contentType("application/json").content(body("alice")))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.username").value("alice"))
            .andExpect(jsonPath("$.password").doesNotExist()).andExpect(jsonPath("$.passwordHash").doesNotExist());
        String hash = accounts.findByUsername("alice").orElseThrow().passwordHash;
        assertTrue(hash.startsWith("$2"));assertNotEquals("StrongTestPassword!", hash);
        mvc.perform(post("/users").with(csrf()).contentType("application/json").content(body("alice")))
            .andExpect(status().isConflict());
    }
    @Test void loginPersistsSessionAndRejectsWrongPassword() throws Exception {
        mvc.perform(post("/users").with(csrf()).contentType("application/json").content(body("alice"))).andExpect(status().isCreated());
        mvc.perform(post("/auth/login").with(csrf()).contentType("application/json")
            .content("{\"username\":\"alice\",\"password\":\"wrong\"}")).andExpect(status().isUnauthorized());
        var result = mvc.perform(post("/auth/login").with(csrf()).contentType("application/json")
            .content("{\"username\":\"alice\",\"password\":\"StrongTestPassword!\"}"))
            .andExpect(status().isOk()).andReturn();
        var session = (MockHttpSession)result.getRequest().getSession(false);
        assertNotNull(session);
        mvc.perform(get("/users/me").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.username").value("alice"));
        mvc.perform(post("/auth/logout").session(session).with(csrf())).andExpect(status().isNoContent());
        assertTrue(session.isInvalid());
    }
    @Test void validationCsrfAndUnauthenticatedAccessAreEnforced() throws Exception {
        mvc.perform(get("/users/me")).andExpect(status().isUnauthorized());
        mvc.perform(post("/users").contentType("application/json").content(body("alice"))).andExpect(status().isForbidden());
        mvc.perform(post("/users").with(csrf()).contentType("application/json").content("{}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mvc.perform(get("/auth/csrf")).andExpect(status().isOk()).andExpect(jsonPath("$.token").isString());
    }
}
