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
    public record ProfileInput(@NotBlank @Email @Size(max=254) String email,
                               @NotBlank @Size(max=80) String firstName,
                               @NotBlank @Size(max=80) String surname) {}
    private final AccountRepository accounts;
    private final PasswordEncoder passwords;
    public AccountService(AccountRepository accounts, PasswordEncoder passwords) {
        this.accounts = accounts; this.passwords = passwords;
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
        return accounts.findByUsername(authentication.getName()).orElseThrow(() -> ApiException.missing("Account"));
    }
    @Transactional
    public UserView update(Authentication authentication, ProfileInput input) {
        Account account = current(authentication);
        account.email = input.email().strip().toLowerCase(Locale.ROOT);
        account.firstName = input.firstName().strip(); account.surname = input.surname().strip();
        return view(accounts.saveAndFlush(account));
    }
    public UserView view(Account account) {
        return new UserView(account.id, account.username, account.email, account.firstName, account.surname);
    }
}
