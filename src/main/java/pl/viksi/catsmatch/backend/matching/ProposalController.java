package pl.viksi.catsmatch.backend.matching;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pl.viksi.catsmatch.backend.cats.CatService;

@RestController @RequestMapping("/matches")
public class ProposalController {
    private final MatchingService matches;
    public ProposalController(MatchingService matches) { this.matches = matches; }

    @GetMapping
    public CatService.PageView<MatchingService.PairView> inbox(Authentication auth,
        @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return matches.inbox(auth, page, size);
    }

    @PostMapping("/{id}/decision")
    public MatchingService.PairView decide(@PathVariable long id, Authentication auth,
        @Valid @RequestBody MatchingService.Decision input) { return matches.decide(id, auth, input); }
}
