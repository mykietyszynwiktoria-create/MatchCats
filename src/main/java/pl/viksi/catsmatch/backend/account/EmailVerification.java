package pl.viksi.catsmatch.backend.account;

import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.viksi.catsmatch.backend.common.ApiException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;

@Service
public class EmailVerification {
    public record Request(@NotBlank @Pattern(regexp="pl|en") String language) {}
    public record Confirm(@NotBlank @Pattern(regexp="[A-Za-z0-9_-]{43}") String token) {}
    private final AccountService service;
    private final AccountRepository accounts;
    private final EmailVerificationRepository tokens;
    private final PasswordRecovery mail;
    private final SecureRandom random=new SecureRandom();
    public EmailVerification(AccountService service,AccountRepository accounts,EmailVerificationRepository tokens,PasswordRecovery mail) {
        this.service=service;this.accounts=accounts;this.tokens=tokens;this.mail=mail;
    }
    @Transactional public void request(Authentication auth,Request input) {
        var account=accounts.lockAccounts(List.of(service.current(auth).id)).getFirst();
        if(account.suspended)throw invalid();
        if(account.emailVerified)return;
        mail.configured();
        Instant now=Instant.now();
        if(tokens.findByAccountId(account.id).filter(t->t.issuedAt.plusSeconds(60).isAfter(now)).isPresent())
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,"TOO_MANY_ATTEMPTS","Wait before requesting another email");
        tokens.deleteAllByAccountId(account.id);tokens.flush();
        byte[] bytes=new byte[32];random.nextBytes(bytes);
        String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokens.saveAndFlush(new EmailVerificationToken(PasswordRecovery.hash(token),account,now));
        String link=mail.link("/#verify/"+token);
        mail.send(account.email,"MatchCats - "+(input.language().equals("pl")?"potwierdź adres e-mail":"verify your email"),
            input.language().equals("pl")?"Aby potwierdzić adres e-mail w MatchCats, otwórz link i wybierz Potwierdź (ważny 24 godziny):\n"+link+"\nJeśli nie proszono o potwierdzenie, zignoruj tę wiadomość.":
            "To verify your MatchCats email, open this link and choose Confirm (valid for 24 hours):\n"+link+"\nIf you did not request verification, ignore this message.");
    }
    @Transactional public void confirm(Confirm input) {
        String hash=PasswordRecovery.hash(input.token());
        var initial=tokens.findById(hash).orElseThrow(this::invalid);
        // Use the same account-first lock order as email changes and resends.
        var account=accounts.lockAccounts(List.of(initial.accountId)).stream().findFirst().orElseThrow(this::invalid);
        var token=tokens.locked(hash).orElseThrow(this::invalid);
        if(account.suspended || !account.email.equals(token.email) || !token.expiresAt.isAfter(Instant.now()))throw invalid();
        account.emailVerified=true;accounts.saveAndFlush(account);
        tokens.delete(token);tokens.flush();
    }
    private ApiException invalid(){return new ApiException(HttpStatus.BAD_REQUEST,"INVALID_VERIFICATION_LINK","Verification link is invalid or expired");}
}
