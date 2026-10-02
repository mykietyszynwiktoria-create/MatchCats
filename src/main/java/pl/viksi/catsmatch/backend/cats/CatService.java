package pl.viksi.catsmatch.backend.cats;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.viksi.catsmatch.backend.account.AccountService;
import pl.viksi.catsmatch.backend.common.ApiException;
import java.time.LocalDate;
import java.util.*;

@Service
public class CatService {
    public record BreederInput(@NotBlank @Size(max=120) String kennel,
        @NotBlank @Size(max=100) String city, @NotBlank @Size(max=100) String country,
        @NotNull @Size(max=2000) String bio) {}
    public record BreederView(Integer id, String kennel, String city, String country, String bio) {}
    public record CatInput(@NotBlank @Size(max=100) String name, @NotBlank @Size(max=100) String breed,
        @NotNull Cat.Sex sex, @NotNull Cat.Health health, @NotNull @PastOrPresent LocalDate birthDate,
        @NotBlank @Size(max=100) String city, @NotBlank @Size(max=100) String country,
        @NotNull @Size(max=2000) String description, @NotNull Boolean available) {}
    public record CatUpdate(@NotNull @PositiveOrZero Long version, @NotNull @Valid CatInput profile) {}
    public record CatView(Integer id, Integer ownerId, String name, String breed, Cat.Sex sex, Cat.Health health,
        LocalDate birthDate, String city, String country, String description, boolean available, long version, boolean hasPhoto) {}
    public record PageView<T>(List<T> items, long total, int page, int size) {}
    private final CatRepository cats;
    private final BreederRepository breeders;
    private final AccountService accounts;
    private final CatPhotoRepository photos;
    public CatService(CatRepository cats, BreederRepository breeders, AccountService accounts, CatPhotoRepository photos) {
        this.cats=cats; this.breeders=breeders; this.accounts=accounts; this.photos=photos;
    }
    public Integer userId(Authentication auth) { return accounts.current(auth).id; }
    public Breeder breeder(Integer id) { return breeders.findById(id).orElseThrow(() -> ApiException.missing("Breeder")); }
    public BreederView breederView(Breeder b) { return new BreederView(b.id,b.kennel,b.city,b.country,b.bio); }
    @Transactional public BreederView saveBreeder(Authentication auth, BreederInput input) {
        Integer id=userId(auth); Breeder b=breeders.findById(id).orElseGet(() -> new Breeder(id));
        b.kennel=input.kennel().strip();b.city=input.city().strip();b.country=input.country().strip();b.bio=input.bio().strip();
        return breederView(breeders.saveAndFlush(b));
    }
    public Cat cat(Integer id) { return cats.findById(id).orElseThrow(() -> ApiException.missing("Cat")); }
    public Cat owned(Integer id, Authentication auth) {
        Cat c=cat(id);if(!c.ownerId.equals(userId(auth))) throw ApiException.forbidden();return c;
    }
    public CatView view(Cat c) { return new CatView(c.id,c.ownerId,c.name,c.breed,c.sex,c.health,c.birthDate,c.city,c.country,c.description,c.available,c.version,photos.existsById(c.id)); }
    private void assign(Cat c, CatInput i) {
        c.name=i.name().strip();c.breed=i.breed().strip();c.sex=i.sex();c.health=i.health();c.birthDate=i.birthDate();
        c.city=i.city().strip();c.country=i.country().strip();c.description=i.description().strip();c.available=i.available();
        if(c.available && c.health!=Cat.Health.HEALTHY)
            throw ApiException.invalid("Available cats must have owner-declared HEALTHY status");
    }
    @Transactional public CatView create(Authentication auth, CatInput input) {
        Integer id=userId(auth); breeder(id); Cat c=new Cat(id);assign(c,input);return view(cats.saveAndFlush(c));
    }
    @Transactional public CatView update(Integer id, Authentication auth, CatUpdate input) {
        Cat c=owned(id,auth);
        if(c.version!=input.version()) throw new ApiException(HttpStatus.CONFLICT,"STALE_VERSION","Refresh the cat profile before saving");
        assign(c,input.profile());return view(cats.saveAndFlush(c));
    }
    @Transactional public void delete(Integer id, Authentication auth) { cats.delete(owned(id,auth));cats.flush(); }
    public PageView<CatView> search(String breed, Cat.Sex sex, String city, Boolean available, Integer ownerId, int page, int size) {
        if(page<0 || size<1 || size>100) throw ApiException.invalid("Page must be non-negative and size between 1 and 100");
        Specification<Cat> spec=(root,q,cb) -> cb.conjunction();
        if(breed!=null) spec=spec.and((r,q,cb)->cb.equal(cb.lower(r.get("breed")),breed.strip().toLowerCase(Locale.ROOT)));
        if(sex!=null) spec=spec.and((r,q,cb)->cb.equal(r.get("sex"),sex));
        if(city!=null) spec=spec.and((r,q,cb)->cb.equal(cb.lower(r.get("city")),city.strip().toLowerCase(Locale.ROOT)));
        if(available!=null) spec=spec.and((r,q,cb)->cb.equal(r.get("available"),available));
        if(ownerId!=null) spec=spec.and((r,q,cb)->cb.equal(r.get("ownerId"),ownerId));
        var results=cats.findAll(spec,PageRequest.of(page,size,Sort.by("id")));
        return new PageView<>(results.getContent().stream().map(this::view).toList(),results.getTotalElements(),page,size);
    }
}
