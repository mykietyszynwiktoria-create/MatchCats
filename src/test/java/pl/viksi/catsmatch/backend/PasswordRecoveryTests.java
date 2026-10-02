package pl.viksi.catsmatch.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import pl.viksi.catsmatch.backend.account.*;
import org.mockito.ArgumentCaptor;
import java.time.Instant;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"app.mail.enabled=true","app.mail.from=test@example.test","app.public-base-url=http://localhost:8084"})
@AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class PasswordRecoveryTests {
    @Autowired MockMvc mvc;
    @Autowired AccountService accounts;
    @Autowired AccountRepository repository;
    @Autowired PasswordResetRepository tokens;
    @MockitoBean JavaMailSender mail;
    String issue() throws Exception {
        mvc.perform(post("/auth/password/request").with(csrf()).contentType("application/json")
            .content("{\"email\":\"recover@example.test\",\"language\":\"en\"}")).andExpect(status().isAccepted());
        var capture=ArgumentCaptor.forClass(SimpleMailMessage.class);verify(mail).send(capture.capture());
        var matcher=Pattern.compile("/#reset/([A-Za-z0-9_-]{43})").matcher(capture.getValue().getText());assertTrue(matcher.find());return matcher.group(1);
    }
    void account() {accounts.register(new AccountService.Registration("recover","OldStrongPassword!","recover@example.test","Test","Breeder"));}
    String reset(String token) {return "{\"token\":\""+token+"\",\"password\":\"NewStrongPassword!\"}";}
    @Test void resetLinksAreHashedSingleUseAndRevokeSessions() throws Exception {
        account();String token=issue();assertTrue(tokens.existsById(PasswordRecovery.hash(token)));assertFalse(tokens.existsById(token));
        long version=repository.findByUsername("recover").orElseThrow().securityVersion;
        mvc.perform(post("/auth/password/reset").with(csrf()).contentType("application/json").content(reset(token))).andExpect(status().isNoContent());
        assertEquals(version+1,repository.findByUsername("recover").orElseThrow().securityVersion);
        mvc.perform(post("/auth/password/reset").with(csrf()).contentType("application/json").content(reset(token)))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_RESET_LINK"));
        mvc.perform(post("/auth/login").with(csrf()).contentType("application/json")
            .content("{\"username\":\"recover\",\"password\":\"OldStrongPassword!\"}")).andExpect(status().isUnauthorized());
        mvc.perform(post("/auth/login").with(csrf()).contentType("application/json")
            .content("{\"username\":\"recover\",\"password\":\"NewStrongPassword!\"}")).andExpect(status().isOk());
    }
    @Test void expiredLinksAreRejected() throws Exception {
        account();String token=issue();var entity=tokens.findById(PasswordRecovery.hash(token)).orElseThrow();entity.expiresAt=Instant.now().minusSeconds(1);tokens.saveAndFlush(entity);
        mvc.perform(post("/auth/password/reset").with(csrf()).contentType("application/json").content(reset(token)))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_RESET_LINK"));
    }
    @Test void unknownEmailHasSameResponseAndCsrfIsRequired() throws Exception {
        mvc.perform(post("/auth/password/request").with(csrf()).contentType("application/json")
            .content("{\"email\":\"missing@example.test\",\"language\":\"en\"}")).andExpect(status().isAccepted());verifyNoInteractions(mail);
        mvc.perform(post("/auth/password/request").contentType("application/json")
            .content("{\"email\":\"missing@example.test\",\"language\":\"en\"}")).andExpect(status().isForbidden());
    }
}
