package pl.viksi.catsmatch.backend.account;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/auth/email")
public class EmailVerificationController {
    private final EmailVerification verification;
    public EmailVerificationController(EmailVerification verification){this.verification=verification;}
    @PostMapping("/request") @ResponseStatus(HttpStatus.ACCEPTED)
    public void request(Authentication auth,@Valid @RequestBody EmailVerification.Request input){verification.request(auth,input);}
    @PostMapping("/confirm") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirm(@Valid @RequestBody EmailVerification.Confirm input){verification.confirm(input);}
}
