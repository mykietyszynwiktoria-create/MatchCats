package pl.viksi.catsmatch.backend.safety;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pl.viksi.catsmatch.backend.cats.CatService;
import java.util.List;

@RestController
public class SafetyController {
    private final SafetyService safety;
    public SafetyController(SafetyService safety){this.safety=safety;}
    @GetMapping("/safety/capabilities") SafetyService.Capabilities capabilities(Authentication auth){return safety.capabilities(auth);}
    @GetMapping("/safety/blocks") List<SafetyService.BlockView> blocks(Authentication auth){return safety.blocks(auth);}
    @GetMapping("/safety/contact/{accountId}") SafetyService.ContactState contact(Authentication auth,@PathVariable int accountId){return safety.contactState(auth,accountId);}
    @PutMapping("/safety/blocks/{accountId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    void block(Authentication auth,@PathVariable int accountId){safety.block(auth,accountId);}
    @DeleteMapping("/safety/blocks/{accountId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    void unblock(Authentication auth,@PathVariable int accountId){safety.unblock(auth,accountId);}
    @PostMapping("/safety/reports") @ResponseStatus(HttpStatus.CREATED)
    SafetyService.ReportView report(Authentication auth,@Valid @RequestBody SafetyService.ReportInput input){return safety.report(auth,input);}
    @GetMapping("/safety/reports") CatService.PageView<SafetyService.ReportView> reports(Authentication auth,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return safety.ownReports(auth,page,size);}
    @GetMapping("/moderation/reports") CatService.PageView<SafetyService.ReportView> queue(Authentication auth,@RequestParam(defaultValue="OPEN") SafetyService.Status status,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return safety.queue(auth,status,page,size);}
    @PostMapping("/moderation/reports/{id}/decision") @ResponseStatus(HttpStatus.NO_CONTENT)
    void decide(Authentication auth,@PathVariable long id,@Valid @RequestBody SafetyService.Decision input){safety.decide(auth,id,input);}
    @GetMapping("/moderation/suspended") List<SafetyService.SuspendedView> suspended(Authentication auth){return safety.suspended(auth);}
    @PostMapping("/moderation/accounts/{id}/reinstate") @ResponseStatus(HttpStatus.NO_CONTENT)
    void reinstate(Authentication auth,@PathVariable int id,@Valid @RequestBody SafetyService.Reinstatement input){safety.reinstate(auth,id,input);}
}
