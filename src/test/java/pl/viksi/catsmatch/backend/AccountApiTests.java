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
    @Test void anonymousVisitorsCanLoadAllEntryPageScripts() throws Exception {
        mvc.perform(get("/health/ready")).andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.database").value("UP"));
        String html=mvc.perform(get("/index.html")).andExpect(status().isOk())
            .andExpect(header().string("Content-Security-Policy", org.hamcrest.Matchers.containsString("default-src 'self'")))
            .andExpect(header().string("Referrer-Policy", "no-referrer"))
            .andExpect(header().string("Permissions-Policy", org.hamcrest.Matchers.containsString("camera=()")))
            .andReturn().getResponse().getContentAsString();
        var scripts=java.util.regex.Pattern.compile("<script\\s+src=\"([^\"]+)\"").matcher(html);
        int count=0;
        while(scripts.find()){
            mvc.perform(get("/"+scripts.group(1))).andExpect(status().isOk());
            count++;
        }
        assertTrue(count>0,"The public entry page must reference application scripts");
        mvc.perform(get("/users/me")).andExpect(status().isUnauthorized());
    }
    @Test void emailVerificationReportsUnavailableDelivery() throws Exception {
        mvc.perform(post("/users").with(csrf()).contentType("application/json").content(body("mailoff")))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.emailVerified").value(false));
        mvc.perform(post("/auth/email/request").with(user("mailoff")).with(csrf()).contentType("application/json").content("{\"language\":\"pl\"}"))
            .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("EMAIL_UNAVAILABLE"));
    }
    @Test void emailChangesRequireCurrentPasswordAndNeverExposeIt() throws Exception {
        mvc.perform(post("/users").with(csrf()).contentType("application/json").content(body("emailtest"))).andExpect(status().isCreated());
        mvc.perform(put("/users/me").with(user("emailtest")).with(csrf()).contentType("application/json")
            .content("{\"email\":\"new@example.test\",\"firstName\":\"Test\",\"surname\":\"Breeder\"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_CURRENT_PASSWORD"));
        mvc.perform(put("/users/me").with(user("emailtest")).with(csrf()).contentType("application/json")
            .content("{\"email\":\"new@example.test\",\"firstName\":\"Test\",\"surname\":\"Breeder\",\"currentPassword\":\"StrongTestPassword!\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("new@example.test")).andExpect(jsonPath("$.currentPassword").doesNotExist());
    }
    @Test void changingPasswordInvalidatesOtherSessionsAndDeletionRequiresPassword() throws Exception {
        mvc.perform(post("/users").with(csrf()).contentType("application/json").content(body("securitytest"))).andExpect(status().isCreated());
        String credentials="{\"username\":\"securitytest\",\"password\":\"StrongTestPassword!\"}";
        var first=(MockHttpSession)mvc.perform(post("/auth/login").with(csrf()).contentType("application/json").content(credentials))
            .andExpect(status().isOk()).andReturn().getRequest().getSession();
        var second=(MockHttpSession)mvc.perform(post("/auth/login").with(csrf()).contentType("application/json").content(credentials))
            .andExpect(status().isOk()).andReturn().getRequest().getSession();
        mvc.perform(post("/users/me/password").session(first).with(csrf()).contentType("application/json")
            .content("{\"currentPassword\":\"wrong\",\"newPassword\":\"NewStrongPassword!\"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_CURRENT_PASSWORD"));
        mvc.perform(post("/users/me/password").session(first).with(csrf()).contentType("application/json")
            .content("{\"currentPassword\":\"StrongTestPassword!\",\"newPassword\":\"NewStrongPassword!\"}"))
            .andExpect(status().isNoContent());
        assertTrue(first.isInvalid());
        mvc.perform(get("/users/me").session(second)).andExpect(status().isUnauthorized());assertTrue(second.isInvalid());
        mvc.perform(post("/auth/login").with(csrf()).contentType("application/json").content(credentials)).andExpect(status().isUnauthorized());
        var latest=(MockHttpSession)mvc.perform(post("/auth/login").with(csrf()).contentType("application/json")
            .content("{\"username\":\"securitytest\",\"password\":\"NewStrongPassword!\"}"))
            .andExpect(status().isOk()).andReturn().getRequest().getSession();
        mvc.perform(delete("/users/me").session(latest).with(csrf()).contentType("application/json").content("{\"currentPassword\":\"wrong\"}"))
            .andExpect(status().isBadRequest());assertTrue(accounts.existsByUsername("securitytest"));
        mvc.perform(delete("/users/me").session(latest).with(csrf()).contentType("application/json").content("{\"currentPassword\":\"NewStrongPassword!\"}"))
            .andExpect(status().isNoContent());assertFalse(accounts.existsByUsername("securitytest"));assertTrue(latest.isInvalid());
    }
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

