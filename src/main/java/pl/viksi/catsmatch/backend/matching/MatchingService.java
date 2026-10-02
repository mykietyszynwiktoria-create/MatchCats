package pl.viksi.catsmatch.backend.matching;

import jakarta.validation.constraints.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.viksi.catsmatch.backend.cats.*;
import pl.viksi.catsmatch.backend.chat.ChatService;
import pl.viksi.catsmatch.backend.common.ApiException;
import java.time.Instant;
import java.util.*;

@Service
public class MatchingService {
    public record Selection(@NotNull @Positive Integer candidateId) {}
    public record PairView(Long id, Integer firstCatId, Integer secondCatId, Long conversationId, Instant createdAt) {}
    private final CatService cats;
    private final CatRepository repository;
    private final CatPairRepository pairs;
    private final ChatService chats;
    private final pl.viksi.catsmatch.backend.safety.SafetyService safety;

    public MatchingService(CatService cats, CatRepository repository, CatPairRepository pairs, ChatService chats, pl.viksi.catsmatch.backend.safety.SafetyService safety) {
        this.cats = cats;
        this.repository = repository;
        this.pairs = pairs;
        this.chats = chats;
        this.safety = safety;
    }

    private void available(Cat cat) {
        if (!cat.available || cat.health != Cat.Health.HEALTHY) {
            throw ApiException.invalid("Both cats must be available with owner-declared HEALTHY status");
        }
    }

    private Pageable page(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw ApiException.invalid("Page must be non-negative and size between 1 and 100");
        }
        return PageRequest.of(page, size, Sort.by("id"));
    }

    @Transactional(readOnly = true)
    public CatService.PageView<CatService.CatView> candidates(int sourceId, Authentication auth, int page, int size) {
        Cat source = cats.owned(sourceId, auth);
        available(source);
        Specification<Cat> spec = (root, query, cb) -> cb.and(
            cb.notEqual(root.get("ownerId"), source.ownerId),
            cb.equal(cb.lower(root.get("breed")), source.breed.toLowerCase(Locale.ROOT)),
            cb.notEqual(root.get("sex"), source.sex),
            cb.isTrue(root.get("available")),
            cb.equal(root.get("health"), Cat.Health.HEALTHY));
        var excluded=safety.excludedContacts(source.ownerId);
        if(!excluded.isEmpty())spec=spec.and((root,query,cb)->cb.not(root.get("ownerId").in(excluded)));
        spec=spec.and((root,query,cb)->{
            var suspended=query.subquery(Integer.class);var account=suspended.from(pl.viksi.catsmatch.backend.account.Account.class);
            suspended.select(account.get("id")).where(cb.isTrue(account.get("suspended")));
            return cb.not(root.get("ownerId").in(suspended));
        });
        var result = repository.findAll(spec, page(page, size));
        return new CatService.PageView<>(result.map(cats::view).getContent(), result.getTotalElements(), page, size);
    }

    @Transactional
    public PairView select(int sourceId, Authentication auth, Selection input) {
        // Lock both cats in ID order before loading them into this transaction.
        // This also serializes opposite-direction requests for the same pair.
        List<Cat> locked = repository.lockCats(List.of(sourceId, input.candidateId()));
        Cat source = locked.stream().filter(c -> c.id.equals(sourceId)).findFirst()
            .orElseThrow(() -> ApiException.missing("Cat"));
        if (!source.ownerId.equals(cats.userId(auth))) throw ApiException.forbidden();
        Cat candidate = locked.stream().filter(c -> c.id.equals(input.candidateId())).findFirst()
            .orElseThrow(() -> ApiException.missing("Candidate"));
        if (source.ownerId.equals(candidate.ownerId) || source.sex == candidate.sex
            || !source.breed.equalsIgnoreCase(candidate.breed)) {
            throw ApiException.invalid("Select a cat of the same breed, opposite sex and a different owner");
        }
        available(source);
        available(candidate);
        int first = Math.min(source.id, candidate.id);
        int second = Math.max(source.id, candidate.id);
        long chatId = chats.contact(candidate.id, auth).id();
        CatPair pair = pairs.findByFirstCatIdAndSecondCatId(first, second).orElseGet(() -> {
            return pairs.saveAndFlush(new CatPair(first, second, chatId));
        });
        return view(pair);
    }

    @Transactional(readOnly = true)
    public CatService.PageView<PairView> list(int sourceId, Authentication auth, int page, int size) {
        cats.owned(sourceId, auth);
        var result = pairs.belongingTo(sourceId, page(page, size));
        return new CatService.PageView<>(result.map(this::view).getContent(), result.getTotalElements(), page, size);
    }

    private PairView view(CatPair pair) {
        return new PairView(pair.id, pair.firstCatId, pair.secondCatId, pair.conversationId, pair.createdAt);
    }
}
