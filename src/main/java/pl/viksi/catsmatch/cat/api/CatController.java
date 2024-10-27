package pl.viksi.catsmatch.cat.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import pl.viksi.catsmatch.cat.domain.Cat;
import pl.viksi.catsmatch.cat.domain.MatchCatService;
import pl.viksi.catsmatch.cat.domain.Owner;
import pl.viksi.catsmatch.cat.persistence.CatRepository;
import pl.viksi.catsmatch.cat.persistence.OwnerRepository;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

import static pl.viksi.catsmatch.cat.persistence.CatRepository.log;


@RestController
public class CatController {

    CatRepository repository = new CatRepository();

    OwnerRepository repositoryOwner = new OwnerRepository();

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

        log.info("Starting addCat with changCatNameRequest" + changCatNameRequest);

        Cat newCat = new Cat(changCatNameRequest.ciciuchName, changCatNameRequest.ciciuchRace,
                changCatNameRequest.health, ownerid);

        log.info("Adding new Cat to cat" + newCat);

        int primkey = repository.saveCat(newCat);

        return primkey;
    }

    @DeleteMapping("/cats/{id}")
    public void deleteCat(@RequestBody ChangCatNameRequest changCatNameRequest,@PathVariable int catid) {
        repository.deleteCat(catid);
    }

    @PutMapping("/cats/{id}")
    public void updateCat(@RequestBody UpdateCat updateCat, @PathVariable int id) {
        repository.upDateCat(id, updateCat.health);
    }

    @PutMapping("/owners/{userid}")
    public List<Owner> addOwner(@RequestBody AddOwner addOwner, @PathVariable int userid){
        //repositoryOwner.AddOwner(addOwner.name1, userid);
        return null;
    }

}

