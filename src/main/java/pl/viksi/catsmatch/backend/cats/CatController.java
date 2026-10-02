package pl.viksi.catsmatch.backend.cats;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pl.viksi.catsmatch.backend.common.ApiException;

@RestController
public class CatController {
    private final CatService service;
    public CatController(CatService service) { this.service=service; }
    @GetMapping("/owners/me") CatService.BreederView me(Authentication auth) { return service.breederView(service.breeder(service.userId(auth))); }
    @PutMapping("/owners/me") CatService.BreederView save(Authentication auth,@Valid @RequestBody CatService.BreederInput input) { return service.saveBreeder(auth,input); }
    @GetMapping("/owners/{id}") CatService.BreederView breeder(@PathVariable Integer id) { return service.breederView(service.breeder(id)); }
    @GetMapping("/cats") CatService.PageView<CatService.CatView> list(
        @RequestParam(required=false) String breed,@RequestParam(required=false) Cat.Sex sex,
        @RequestParam(required=false) String city,@RequestParam(required=false) Boolean available,
        @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        return service.search(breed,sex,city,available,null,page,size);
    }
    @GetMapping("/owners/me/cats") CatService.PageView<CatService.CatView> own(Authentication auth,
        @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        return service.search(null,null,null,null,service.userId(auth),page,size);
    }
    @GetMapping("/cats/{id}") CatService.CatView get(@PathVariable Integer id) { return service.view(service.cat(id)); }
    @PostMapping("/cats") @ResponseStatus(HttpStatus.CREATED)
    CatService.CatView create(Authentication auth,@Valid @RequestBody CatService.CatInput input) { return service.create(auth,input); }
    @PutMapping("/cats/{id}") CatService.CatView update(@PathVariable Integer id,Authentication auth,@Valid @RequestBody CatService.CatUpdate input) { return service.update(id,auth,input); }
    @DeleteMapping("/cats/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Integer id,Authentication auth) { service.delete(id,auth); }
}
