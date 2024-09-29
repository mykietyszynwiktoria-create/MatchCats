package pl.viksi.catsmatch.account.api;

import org.springframework.web.bind.annotation.*;
import pl.viksi.catsmatch.account.domain.Cat;
import pl.viksi.catsmatch.account.persistence.CatRepository;

import java.util.ArrayList;
import java.util.List;


@RestController
public class AccountController {

    List<Cat> cats = new ArrayList<Cat>();
    int sequenceNumber = 0;
    CatRepository repository = new CatRepository();

    AccountController(){

    }

    @GetMapping("/cats")
    public List<Cat> generate() {
        return repository.getCats();
    }

    @PostMapping("/cats")
    public int addCat(@RequestBody ChangCatNameRequest changCatNameRequest) {
        Cat newCat = new Cat(changCatNameRequest.ciciuchName, changCatNameRequest.ciciuchRace,
                changCatNameRequest.idCat, changCatNameRequest.health);
        cats.add(newCat);
        int idCat = sequenceNumber;
        newCat.idCat = sequenceNumber++;
        repository.saveCat(newCat);
        return idCat;
    }

    @DeleteMapping("/cats")
    public void deleteCat(@RequestBody ChangCatNameRequest changCatNameRequest) {
        repository.deleteCat(changCatNameRequest.idCat);
    }

    @PutMapping("/cats/{id}")
    public void updateCat(@RequestBody UpdateCat updateCat, @PathVariable int id) {
        repository.upDateCat(id, updateCat.health);
    }

}

