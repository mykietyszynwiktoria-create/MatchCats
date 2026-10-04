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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"app.mail.enabled=true","app.mail.from=test@example.test","app.public-base-url=http://localhost:8084"})
@AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class EmailVerificationTests {
    @Autowired MockMvc mvc;
    @Autowired AccountService accounts;
    @Autowired AccountRepository repository;
    @Autowired EmailVerificationRepository tokens;
    @MockitoBean JavaMailSender mail;
    void account(){accounts.register(new AccountService.Registration("verify","StrongPassword!","verify@example.test","Test","Breeder"));}
    String issue(String language) throws Exception {
        mvc.perform(post("/auth/email/request").with(user("verify")).with(csrf()).contentType("application/json")
            .content("{\"language\":\""+language+"\"}")).andExpect(status().isAccepted());
        var capture=ArgumentCaptor.forClass(SimpleMailMessage.class);verify(mail,atLeastOnce()).send(capture.capture());
        var message=capture.getValue();assertArrayEquals(new String[]{"verify@example.test"},message.getTo());
        assertTrue(message.getText().contains(language.equals("pl")?"24 godziny":"24 hours"));
        var matcher=Pattern.compile("/#verify/([A-Za-z0-9_-]{43})").matcher(message.getText());assertTrue(matcher.find());return matcher.group(1);
    }
    void confirm(String token,int expected) throws Exception {
        mvc.perform(post("/auth/email/confirm").with(csrf()).contentType("application/json")
            .content("{\"token\":\""+token+"\"}")).andExpect(status().is(expected));
    }
    @Test void hashedSingleUseLinkVerifiesOnlyInboxWithoutSigningIn() throws Exception {
        account();String token=issue("en");assertTrue(tokens.existsById(PasswordRecovery.hash(token)));assertFalse(tokens.existsById(token));
        confirm(token,204);assertTrue(repository.findByUsername("verify").orElseThrow().emailVerified);confirm(token,400);
        mvc.perform(get("/users/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/users/me").with(user("verify"))).andExpect(jsonPath("$.emailVerified").value(true));
    }
    @Test void expiryAndSuspensionRejectConfirmation() throws Exception {
        account();String token=issue("pl");var entity=tokens.findById(PasswordRecovery.hash(token)).orElseThrow();
        entity.expiresAt=Instant.now().minusSeconds(1);tokens.saveAndFlush(entity);confirm(token,400);
        entity.expiresAt=Instant.now().plusSeconds(60);tokens.saveAndFlush(entity);
        var account=repository.findByUsername("verify").orElseThrow();account.suspended=true;repository.saveAndFlush(account);confirm(token,400);
        assertFalse(account.emailVerified);
    }
    @Test void resendsAreLimitedAndReplaceOldLink() throws Exception {
        account();String old=issue("pl");
        mvc.perform(post("/auth/email/request").with(user("verify")).with(csrf()).contentType("application/json")
            .content("{\"language\":\"en\"}")).andExpect(status().isTooManyRequests());
        var entity=tokens.findById(PasswordRecovery.hash(old)).orElseThrow();entity.issuedAt=Instant.now().minusSeconds(61);tokens.saveAndFlush(entity);
        String replacement=issue("en");assertNotEquals(old,replacement);confirm(old,400);confirm(replacement,204);
    }
    @Test void emailChangesInvalidateLinksAndClearVerifiedStatus() throws Exception {
        account();String old=issue("en");
        mvc.perform(put("/users/me").with(user("verify")).with(csrf()).contentType("application/json")
            .content("{\"email\":\"new@example.test\",\"firstName\":\"Test\",\"surname\":\"Breeder\",\"currentPassword\":\"StrongPassword!\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.emailVerified").value(false));confirm(old,400);
        var account=repository.findByUsername("verify").orElseThrow();account.emailVerified=true;repository.saveAndFlush(account);
        mvc.perform(put("/users/me").with(user("verify")).with(csrf()).contentType("application/json")
            .content("{\"email\":\"third@example.test\",\"firstName\":\"Test\",\"surname\":\"Breeder\",\"currentPassword\":\"StrongPassword!\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.emailVerified").value(false));
    }
    @Test void authCsrfAndTokenFormatAreRequired() throws Exception {
        account();String token=issue("en");
        mvc.perform(post("/auth/email/request").with(csrf()).contentType("application/json").content("{\"language\":\"en\"}"))
            .andExpect(status().isUnauthorized());
        mvc.perform(post("/auth/email/request").with(user("verify")).contentType("application/json").content("{\"language\":\"en\"}"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/auth/email/confirm").contentType("application/json").content("{\"token\":\""+token+"\"}"))
            .andExpect(status().isForbidden());confirm("invalid",400);
    }
    @Test void verifiedAccountDoesNotSendAnotherMessage() throws Exception {
        account();var account=repository.findByUsername("verify").orElseThrow();account.emailVerified=true;repository.saveAndFlush(account);
        mvc.perform(post("/auth/email/request").with(user("verify")).with(csrf()).contentType("application/json").content("{\"language\":\"en\"}"))
            .andExpect(status().isAccepted());verifyNoInteractions(mail);assertTrue(tokens.findByAccountId(account.id).isEmpty());
    }
    @Test @Transactional(propagation=org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void failedDeliveryRollsBackReplacement() throws Exception {
        String name="mail"+java.util.UUID.randomUUID().toString().replace("-","");
        int id=accounts.register(new AccountService.Registration(name,"StrongPassword!",name+"@example.test","Test","Breeder")).id();
        try {
            String raw="B".repeat(43),hash=PasswordRecovery.hash(raw);
            tokens.saveAndFlush(new EmailVerificationToken(hash,repository.findById(id).orElseThrow(),Instant.now().minusSeconds(61)));
            doThrow(new org.springframework.mail.MailSendException("Test delivery failed")).when(mail).send(any(SimpleMailMessage.class));
            mvc.perform(post("/auth/email/request").with(user(name)).with(csrf()).contentType("application/json").content("{\"language\":\"en\"}"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("EMAIL_UNAVAILABLE"));
            assertEquals(hash,tokens.findByAccountId(id).orElseThrow().tokenHash,"Failed delivery must preserve the previous link");
            confirm(raw,204);
        } finally {repository.deleteById(id);}
    }
}
