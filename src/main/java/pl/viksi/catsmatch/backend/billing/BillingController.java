package pl.viksi.catsmatch.backend.billing;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pl.viksi.catsmatch.backend.common.ApiException;

import java.util.List;

@RestController
public class BillingController {
    public record Plan(String id, String name, String price, boolean available, List<String> features) {}
    public record AccountPlan(String planId, boolean active, String status) {}

    private static final List<Plan> PLANS = List.of(
        new Plan("FREE", "Free", "0", true, List.of("Breeder and cat profiles", "Candidate search", "Conversations and pair proposals")),
        new Plan("PREMIUM", "Premium", "COMING_SOON", false, List.of("More profiles and documents", "Extended search filters", "Cattery highlighting"))
    );

    @GetMapping("/billing/plans")
    List<Plan> plans() { return PLANS; }

    @GetMapping("/billing/me")
    AccountPlan current(Authentication authentication) {
        return new AccountPlan("FREE", true, "ACTIVE");
    }

    @PostMapping("/billing/checkout")
    @ResponseStatus(HttpStatus.CONFLICT)
    void checkout(Authentication authentication) {
        throw new ApiException(HttpStatus.CONFLICT, "PAYMENT_NOT_CONFIGURED", "Payment provider is not configured");
    }
}

