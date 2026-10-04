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
    public enum Action { ACCEPT, DECLINE, WITHDRAW }
    public record Decision(@NotNull Action action) {}
    public record PairView(Long id, Integer firstCatId, Integer secondCatId, Long conversationId, Instant createdAt,
        CatPair.Status status, Integer proposedBy, Instant decidedAt, boolean canDecide, boolean canWithdraw) {}
    private final CatService cats;
    private final CatRepository repository;
    private final CatPairRepository pairs;
    private final ChatService chats;
    private final pl.viksi.catsmatch.backend.account.AccountRepository accounts;
    private final pl.viksi.catsmatch.backend.safety.SafetyService safety;

    public MatchingService(CatService cats, CatRepository repository, CatPairRepository pairs, ChatService chats, pl.viksi.catsmatch.backend.safety.SafetyService safety,
        pl.viksi.catsmatch.backend.account.AccountRepository accounts) {
        this.cats = cats;
        this.repository = repository;
        this.pairs = pairs;
        this.chats = chats;
        this.safety = safety;
        this.accounts = accounts;
    }

    private List<Cat> lockParticipants(List<Integer> catIds) {
        // Account deletion locks the owner before cascading to cats. Use that
        // same order here, before contact() reacquires the account locks.
        // Read scalar IDs only, so cats are loaded after any deletion finishes.
        var ownerIds = repository.ownerIds(catIds);
        if (!ownerIds.isEmpty()) accounts.lockAccounts(ownerIds);
        return repository.lockCats(catIds);
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
        List<Cat> locked = lockParticipants(List.of(sourceId, input.candidateId()));
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
        // A migrated, one-sided saved pair needs an explicit new proposal.
        if (pair.proposedBy == null && pair.status == CatPair.Status.PENDING) pair.proposedBy = source.ownerId;
        return view(pair, source.ownerId);
    }

    @Transactional(readOnly = true)
    public CatService.PageView<PairView> list(int sourceId, Authentication auth, int page, int size) {
        int owner = cats.owned(sourceId, auth).ownerId;
        var result = pairs.belongingTo(sourceId, page(page, size));
        return new CatService.PageView<>(result.map(p -> view(p, owner)).getContent(), result.getTotalElements(), page, size);
    }

    @Transactional(readOnly = true)
    public CatService.PageView<PairView> inbox(Authentication auth, int page, int size) {
        int owner = cats.userId(auth);
        var result = pairs.belongingToOwner(owner, page(page, size));
        return new CatService.PageView<>(result.map(p -> view(p, owner)).getContent(), result.getTotalElements(), page, size);
    }

    @Transactional
    public PairView decide(long id, Authentication auth, Decision input) {
        int owner = cats.userId(auth);
        // Read only IDs before locking, avoiding a stale managed pair after waiting.
        var ids = pairs.catIds(id).orElseThrow(() -> ApiException.missing("Proposal"));
        var locked = lockParticipants(List.of(ids.getFirstCatId(), ids.getSecondCatId()));
        Cat first = locked.stream().filter(c -> c.id.equals(ids.getFirstCatId())).findFirst()
            .orElseThrow(() -> ApiException.missing("Cat"));
        Cat second = locked.stream().filter(c -> c.id.equals(ids.getSecondCatId())).findFirst()
            .orElseThrow(() -> ApiException.missing("Cat"));
        if (!first.ownerId.equals(owner) && !second.ownerId.equals(owner)) throw ApiException.forbidden();
        CatPair pair = pairs.findById(id).orElseThrow(() -> ApiException.missing("Proposal"));
        boolean receiver = pair.proposedBy != null && !pair.proposedBy.equals(owner);
        if (input.action() == Action.WITHDRAW) {
            if (!(pair.status == CatPair.Status.ACCEPTED ||
                (pair.status == CatPair.Status.PENDING && ownerEquals(pair.proposedBy, owner)))) throw conflict();
            pair.status = CatPair.Status.WITHDRAWN;
        } else {
            if (!receiver) throw ApiException.forbidden();
            if (pair.status != CatPair.Status.PENDING) throw conflict();
            if (input.action() == Action.ACCEPT) {
                available(first); available(second);
                if (first.sex == second.sex || !first.breed.equalsIgnoreCase(second.breed))
                    throw ApiException.invalid("The cats are no longer compatible");
                // Recheck contact permissions under the same locks used by blocking.
                chats.contact(first.ownerId.equals(owner) ? second.id : first.id, auth);
                pair.status = CatPair.Status.ACCEPTED;
            } else pair.status = CatPair.Status.DECLINED;
        }
        pair.decidedAt = Instant.now();
        return view(pair, owner);
    }

    private boolean ownerEquals(Integer proposer, int owner) { return proposer != null && proposer.equals(owner); }
    private ApiException conflict() {
        return new ApiException(org.springframework.http.HttpStatus.CONFLICT, "PROPOSAL_CHANGED", "Refresh the proposal before deciding");
    }
    private PairView view(CatPair pair, int owner) {
        boolean pending = pair.status == CatPair.Status.PENDING && pair.proposedBy != null;
        return new PairView(pair.id, pair.firstCatId, pair.secondCatId, pair.conversationId, pair.createdAt,
            pair.status, pair.proposedBy, pair.decidedAt, pending && !ownerEquals(pair.proposedBy, owner),
            pair.status == CatPair.Status.ACCEPTED || (pending && ownerEquals(pair.proposedBy, owner)));
    }
}
