package pl.viksi.catsmatch.backend.account;

import jakarta.validation.constraints.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.viksi.catsmatch.backend.common.ApiException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.util.*;

@Service
public class PasswordRecovery {
    public record Request(@NotBlank @Email @Size(max=254) String email,@NotBlank @Pattern(regexp="pl|en") String language) {}
    public record Reset(@NotBlank @Pattern(regexp="[A-Za-z0-9_-]{43}") String token,@NotBlank @Size(min=8,max=72) String password) {}
    private final AccountRepository accounts;
    private final AccountService service;
    private final PasswordResetRepository tokens;
    private final ObjectProvider<JavaMailSender> sender;
    private final boolean enabled;
    private final String from,base;
    private final SecureRandom random=new SecureRandom();
    public PasswordRecovery(AccountRepository accounts,AccountService service,PasswordResetRepository tokens,
        ObjectProvider<JavaMailSender> sender,@Value("${app.mail.enabled:false}") boolean enabled,
        @Value("${app.mail.from:}") String from,@Value("${app.public-base-url:}") String base) {
        this.accounts=accounts;this.service=service;this.tokens=tokens;this.sender=sender;this.enabled=enabled;this.from=from;this.base=base.replaceAll("/+$","");
    }
    public static String hash(String value) {
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}
        catch(NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    }
    void configured() {
        boolean safe=false;
        try{var uri=URI.create(base);safe=uri.getHost()!=null && uri.getUserInfo()==null && uri.getFragment()==null && uri.getQuery()==null
            && ("https".equals(uri.getScheme()) || ("http".equals(uri.getScheme()) && Set.of("localhost","127.0.0.1").contains(uri.getHost())));}catch(IllegalArgumentException ignored){}
        if(!enabled || from.isBlank() || !safe || sender.getIfAvailable()==null)
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"EMAIL_UNAVAILABLE","Email delivery is not configured");
    }
    String link(String fragment){return base+fragment;}
    void send(String address,String subject,String text) {
        var mail=new SimpleMailMessage();mail.setFrom(from);mail.setTo(address);mail.setSubject(subject);mail.setText(text);
        try{sender.getObject().send(mail);}catch(MailException ex){throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"EMAIL_UNAVAILABLE","Email delivery is temporarily unavailable");}
    }
    @Transactional public void request(Request input) {
        configured();
        var found=accounts.lockByEmail(input.email().strip().toLowerCase(Locale.ROOT));
        if(found.isEmpty())return; // Same success response for an unknown address.
        Account account=found.get();if(account.suspended)return;
        tokens.deleteAllByAccountId(account.id);tokens.flush();
        byte[] bytes=new byte[32];random.nextBytes(bytes);String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokens.saveAndFlush(new PasswordResetToken(hash(token),account.id,Instant.now().plusSeconds(900)));
        String link=base+"/#reset/"+token;
        send(account.email,"MatchCats - password reset",input.language().equals("pl")?"Aby ustawić nowe hasło MatchCats, otwórz link (ważny 15 minut):\n"+link+"\nJeśli nie proszono o zmianę, zignoruj tę wiadomość.":"To reset your MatchCats password, open this link (valid for 15 minutes):\n"+link+"\nIf you did not request this, ignore this message.");
    }
    @Transactional public void reset(Reset input) {
        String hash=hash(input.token());
        var initial=tokens.findById(hash).orElseThrow(this::invalid);
        var account=accounts.lockAccounts(List.of(initial.accountId)).stream().findFirst().orElseThrow(this::invalid);
        var token=tokens.locked(hash).orElseThrow(this::invalid);
        if(!token.expiresAt.isAfter(Instant.now()))throw invalid();
        service.replacePassword(account,input.password());
    }
    private ApiException invalid(){return new ApiException(HttpStatus.BAD_REQUEST,"INVALID_RESET_LINK","Reset link is invalid or expired");}
}
