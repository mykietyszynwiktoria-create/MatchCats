package pl.viksi.catsmatch.backend.matching;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pl.viksi.catsmatch.backend.cats.CatService;

@RestController
@RequestMapping("/cats/{sourceId}")
public class MatchingController {
    private final MatchingService matches;
    public MatchingController(MatchingService matches) { this.matches = matches; }

    @GetMapping("/candidates")
    public CatService.PageView<CatService.CatView> candidates(@PathVariable int sourceId, Authentication auth,
        @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return matches.candidates(sourceId, auth, page, size);
    }

    @PostMapping("/matches")
    public MatchingService.PairView select(@PathVariable int sourceId, Authentication auth,
                                          @Valid @RequestBody MatchingService.Selection input) {
        return matches.select(sourceId, auth, input);
    }

    @GetMapping("/matches")
    public CatService.PageView<MatchingService.PairView> list(@PathVariable int sourceId, Authentication auth,
        @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return matches.list(sourceId, auth, page, size);
    }
}
