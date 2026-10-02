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
    public record Login(@JsonAlias("nick_login") @NotBlank @Size(max=40) String username,
                        @JsonAlias("login_password") @NotBlank @Size(max=72) String password) {}
    private final AccountService service;
    private final AuthenticationManager manager;
    private final SecurityContextRepository contexts;
    private final pl.viksi.catsmatch.backend.security.LoginThrottle attempts;
    public AccountController(AccountService service, AuthenticationManager manager, SecurityContextRepository contexts,
                             pl.viksi.catsmatch.backend.security.LoginThrottle attempts) {
        this.service = service; this.manager = manager; this.contexts = contexts; this.attempts=attempts;
    }
    @GetMapping("/health") Map<String,String> health() { return Map.of("status","UP"); }
    @GetMapping("/auth/csrf") CsrfToken csrf(CsrfToken token) { return token; }
    @PostMapping("/users") @ResponseStatus(HttpStatus.CREATED)
    AccountService.UserView register(@Valid @RequestBody AccountService.Registration input) { return service.register(input); }
    @PostMapping("/auth/login")
    @org.springframework.transaction.annotation.Transactional
    AccountService.UserView login(@Valid @RequestBody Login input, HttpServletRequest request, HttpServletResponse response) {
        attempts.check(request.getRemoteAddr(),input.username());
        service.lockLogin(input.username());
        Authentication auth = manager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(input.username(), input.password()));
        attempts.success(request.getRemoteAddr(),input.username());
        if (request.getSession(false) != null) request.changeSessionId();
        new CsrfAuthenticationStrategy(new HttpSessionCsrfTokenRepository()).onAuthentication(auth, request, response);
        var context = SecurityContextHolder.createEmptyContext(); context.setAuthentication(auth);
        SecurityContextHolder.setContext(context); contexts.saveContext(context, request, response);
        request.getSession().setAttribute(pl.viksi.catsmatch.backend.security.SessionVersionFilter.ATTRIBUTE,service.current(auth).securityVersion);
        return service.view(service.current(auth));
    }
    @GetMapping({"/users/me", "/auth/me"})
    AccountService.UserView me(Authentication auth) { return service.view(service.current(auth)); }
    @PutMapping("/users/me")
    AccountService.UserView update(Authentication auth, @Valid @RequestBody AccountService.ProfileInput input) {
        return service.update(auth, input);
    }
    private void endSession(HttpServletRequest request) {
        if(request.getSession(false)!=null)request.getSession(false).invalidate();SecurityContextHolder.clearContext();
    }
    @PostMapping("/users/me/password") @ResponseStatus(HttpStatus.NO_CONTENT)
    void password(Authentication auth,@Valid @RequestBody AccountService.PasswordChange input,HttpServletRequest request) {
        service.changePassword(auth,input);endSession(request);
    }
    @DeleteMapping("/users/me") @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(Authentication auth,@Valid @RequestBody AccountService.DeleteAccount input,HttpServletRequest request) {
        service.delete(auth,input);endSession(request);
    }
}
