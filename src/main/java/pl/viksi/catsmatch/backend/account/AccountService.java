package pl.viksi.catsmatch.backend.account;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.viksi.catsmatch.backend.common.ApiException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class AccountService {
    public record Registration(
        @JsonAlias("nick_login") @NotBlank @Pattern(regexp="[a-zA-Z0-9._-]{3,40}") String username,
        @JsonAlias("login_password") @NotBlank @Size(min=8,max=72) String password,
        @NotBlank @Email @Size(max=254) String email,
        @JsonAlias("firstname") @NotBlank @Size(max=80) String firstName,
        @NotBlank @Size(max=80) String surname) {}
    public record UserView(Integer id, String username, String email, String firstName, String surname) {}
    public record PasswordChange(@NotBlank String currentPassword, @NotBlank @Size(min=8,max=72) String newPassword) {}
    public record DeleteAccount(@NotBlank String currentPassword) {}
    public record ProfileInput(@NotBlank @Email @Size(max=254) String email,
                               @NotBlank @Size(max=80) String firstName,
                               @NotBlank @Size(max=80) String surname,@Size(max=72) String currentPassword) {}
    private final AccountRepository accounts;
    private final PasswordEncoder passwords;
    private final PasswordResetRepository resets;
    public AccountService(AccountRepository accounts, PasswordEncoder passwords,PasswordResetRepository resets) {
        this.accounts = accounts; this.passwords = passwords;this.resets=resets;
    }
    @Transactional
    public UserView register(Registration input) {
        if (input.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw ApiException.invalid("Password must not exceed 72 UTF-8 bytes");
        }
        String username = input.username().toLowerCase(Locale.ROOT);
        String email = input.email().strip().toLowerCase(Locale.ROOT);
        if (accounts.existsByUsername(username) || accounts.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "ACCOUNT_EXISTS", "Username or email is already registered");
        }
        return view(accounts.saveAndFlush(new Account(username, passwords.encode(input.password()),
                email, input.firstName().strip(), input.surname().strip())));
    }
    public Account current(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Sign in first");
        }
        Account account=accounts.findByUsername(authentication.getName()).orElseThrow(() -> ApiException.missing("Account"));
        if(account.suspended)throw new ApiException(HttpStatus.FORBIDDEN,"ACCOUNT_SUSPENDED","Account access is suspended");
        return account;
    }
    public void lockLogin(String username){accounts.lockByUsername(username.toLowerCase(Locale.ROOT));}
    public boolean currentOwnerSuspended(Integer id){return accounts.findById(id).map(a->a.suspended).orElse(true);}
    @Transactional
    public UserView update(Authentication authentication, ProfileInput input) {
        Account account = accounts.lockAccounts(java.util.List.of(current(authentication).id)).getFirst();
        String email=input.email().strip().toLowerCase(Locale.ROOT);
        if(!account.email.equals(email)){verify(account,input.currentPassword());resets.deleteAllByAccountId(account.id);resets.flush();}
        account.email = email;
        account.firstName = input.firstName().strip(); account.surname = input.surname().strip();
        return view(accounts.saveAndFlush(account));
    }
    public UserView view(Account account) {
        return new UserView(account.id, account.username, account.email, account.firstName, account.surname);
    }
    private void verify(Account account,String password) {
        if(password==null || password.isBlank() || password.getBytes(StandardCharsets.UTF_8).length>72 || !passwords.matches(password,account.passwordHash))
            throw new ApiException(HttpStatus.BAD_REQUEST,"INVALID_CURRENT_PASSWORD","Current password is incorrect");
    }
    @Transactional public void changePassword(Authentication auth,PasswordChange input) {
        Account account=accounts.lockAccounts(java.util.List.of(current(auth).id)).getFirst();verify(account,input.currentPassword());
        replacePassword(account,input.newPassword());
    }
    public void replacePassword(Account account,String password) {
        if(password.getBytes(StandardCharsets.UTF_8).length>72)throw ApiException.invalid("Password must not exceed 72 UTF-8 bytes");
        account.passwordHash=passwords.encode(password);account.securityVersion++;
        resets.deleteAllByAccountId(account.id);resets.flush();
        accounts.saveAndFlush(account);
    }
    @Transactional public void delete(Authentication auth,DeleteAccount input) {
        Account account=accounts.lockAccounts(java.util.List.of(current(auth).id)).getFirst();verify(account,input.currentPassword());accounts.delete(account);accounts.flush();
    }
}
