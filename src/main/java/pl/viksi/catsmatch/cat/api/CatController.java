package pl.viksi.catsmatch.cat.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import pl.viksi.catsmatch.cat.domain.Cat;
import pl.viksi.catsmatch.cat.domain.MatchCatService;
import pl.viksi.catsmatch.cat.persistence.CatRepository;

import java.util.ArrayList;
import java.util.List;


@RestController
public class CatController {

    List<Cat> cats = new ArrayList<Cat>();
    int sequenceNumber = 0;
    CatRepository repository = new CatRepository();

    @Autowired
    MatchCatService matchCatService;

    CatController(){

    }

    @PostMapping("/cats/{id}/matches")
    public List<Cat> getMatches(@PathVariable int id) {
        return matchCatService.getMatchedAndGetMatches(id);
    }

    @PostMapping("/owners/{ownerid}/cats")
    public int addCat(@RequestBody ChangCatNameRequest changCatNameRequest, @PathVariable int ownerid) {
        Cat newCat = new Cat(changCatNameRequest.ciciuchName, changCatNameRequest.ciciuchRace,
                changCatNameRequest.idCat, changCatNameRequest.health, ownerid);
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

