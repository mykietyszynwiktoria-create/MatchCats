package pl.viksi.catsmatch.cat.api;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pl.viksi.catsmatch.cat.domain.Cat;
import pl.viksi.catsmatch.cat.domain.CustomerException;
import pl.viksi.catsmatch.cat.domain.MatchCatService;
import pl.viksi.catsmatch.cat.domain.Owner;
import pl.viksi.catsmatch.cat.persistence.CatRepository;
import pl.viksi.catsmatch.cat.persistence.OwnerRepository;
import pl.viksi.catsmatch.cat.persistence.RelationShipCatsRepository;
import pl.viksi.catsmatch.user.domain.UserService;

import java.sql.SQLException;
import java.util.List;

import static pl.viksi.catsmatch.cat.persistence.CatRepository.log;


@RestController
public class CatController {

    CatRepository repository = new CatRepository();

    @Autowired
    OwnerRepository repositoryOwner;

    @Autowired
    UserService userService;

    @Autowired
    MatchCatService matchCatService;

    @ResponseStatus(value = HttpStatus.BAD_REQUEST,
            reason = "user error")
    @ExceptionHandler(CustomerException.class)
    public void badRequest(CustomerException customerException) {
        log.warn("CustomerException occurred: ", customerException);
    }

    @PostMapping("/cats/{id}/matches")
    public List<Cat> getMatches(@PathVariable int id) {
        return matchCatService.createNewMatches(id);
    }

    @PostMapping("/owners/{ownerid}/cats")
    public int addCat(@Valid @RequestBody ChangCatNameRequest changCatNameRequest, @PathVariable int ownerid) throws SQLException {

        log.info("Starting addCat with changCatNameRequest" + changCatNameRequest);

        if (repositoryOwner.findOwner(ownerid) == null) {
            throw new CustomerException("no owner found");
        }

        Cat newCat = new Cat(changCatNameRequest.ciciuchName, changCatNameRequest.ciciuchRace,
                changCatNameRequest.health,changCatNameRequest.sex, ownerid);

        log.info("Adding new Cat to cat" + newCat);

        int primkey = repository.saveCat(newCat);

        return primkey;
    }

    @DeleteMapping("/cats/{catid}")
    public void deleteCat(@PathVariable int catid) {
        repository.deleteCat(catid);
    }

    @PutMapping("/cats/{id}")
    public void updateCat(@RequestBody UpdateCat updateCat, @PathVariable int id) {
        repository.upDateCat(id, updateCat.health);
    }

    @PutMapping("/owners/{userid}")
    public Owner addOwner(@RequestBody AddOwner addOwner, @PathVariable int userid) {

        if (!userService.existUser(userid)) {
            log.warn(" Add owner: user not found. Userid = " + userid);
            throw new CustomerException("no user found ");

        }

        try {

            Owner owner;
            if (repositoryOwner.findOwner(userid) == null) {
                log.info("Show not found owner =" + userid);
                owner = new Owner(userid, 0, 0, addOwner.name);
                log.info("Show new owner =" + owner);
                return repositoryOwner.insertOwner(owner);

            } else {
                log.info("show exist owner ="+ userid);

                return repositoryOwner.findOwner(userid);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }
}

