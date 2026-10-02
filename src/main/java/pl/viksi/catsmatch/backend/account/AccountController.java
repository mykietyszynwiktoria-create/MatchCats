package pl.viksi.catsmatch.backend.account;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.core.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.*;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
public class AccountController {
    public record Login(@JsonAlias("nick_login") @NotBlank String username,
                        @JsonAlias("login_password") @NotBlank String password) {}
    private final AccountService service;
    private final AuthenticationManager manager;
    private final SecurityContextRepository contexts;
    public AccountController(AccountService service, AuthenticationManager manager, SecurityContextRepository contexts) {
        this.service = service; this.manager = manager; this.contexts = contexts;
    }
    @GetMapping("/health") Map<String,String> health() { return Map.of("status","UP"); }
    @GetMapping("/auth/csrf") CsrfToken csrf(CsrfToken token) { return token; }
    @PostMapping("/users") @ResponseStatus(HttpStatus.CREATED)
    AccountService.UserView register(@Valid @RequestBody AccountService.Registration input) { return service.register(input); }
    @PostMapping("/auth/login")
    AccountService.UserView login(@Valid @RequestBody Login input, HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = manager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(input.username(), input.password()));
        if (request.getSession(false) != null) request.changeSessionId();
        new CsrfAuthenticationStrategy(new HttpSessionCsrfTokenRepository()).onAuthentication(auth, request, response);
        var context = SecurityContextHolder.createEmptyContext(); context.setAuthentication(auth);
        SecurityContextHolder.setContext(context); contexts.saveContext(context, request, response);
        return service.view(service.current(auth));
    }
    @GetMapping({"/users/me", "/auth/me"})
    AccountService.UserView me(Authentication auth) { return service.view(service.current(auth)); }
    @PutMapping("/users/me")
    AccountService.UserView update(Authentication auth, @Valid @RequestBody AccountService.ProfileInput input) {
        return service.update(auth, input);
    }
}
