package pl.viksi.catsmatch.backend.account;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import pl.viksi.catsmatch.backend.security.LoginThrottle;

@RestController @RequestMapping("/auth/password")
public class PasswordRecoveryController {
    private final PasswordRecovery recovery;
    private final LoginThrottle attempts;
    public PasswordRecoveryController(PasswordRecovery recovery,LoginThrottle attempts){this.recovery=recovery;this.attempts=attempts;}
    @PostMapping("/request") @ResponseStatus(HttpStatus.ACCEPTED)
    public void request(@Valid @RequestBody PasswordRecovery.Request input,HttpServletRequest request){
        attempts.check("reset:"+request.getRemoteAddr(),input.email());recovery.request(input);
    }
    @PostMapping("/reset") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset(@Valid @RequestBody PasswordRecovery.Reset input,HttpServletRequest request){
        recovery.reset(input);if(request.getSession(false)!=null)request.getSession(false).invalidate();SecurityContextHolder.clearContext();
    }
}
