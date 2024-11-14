package pl.viksi.catsmatch.cat.domain;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.viksi.catsmatch.cat.persistence.CatRepository;
import pl.viksi.catsmatch.cat.persistence.RelationShipCatsRepository;
import pl.viksi.catsmatch.user.domain.UserService;

import java.util.ArrayList;
import java.util.List;

@Component
public class MatchCatService {


    private static final Log log = LogFactory.getLog(MatchCatService.class);
    CatRepository repository = new CatRepository();

    @Autowired
    RelationShipCatsRepository relationRepository;
    UserService userService = new UserService();

    public List<Cat> createNewMatches(int idCat) {

        log.info("Getting matchedCats with catId: " + idCat);

        Cat cat = repository.getCat(idCat);
        log.info("cat returned: " + cat);

        List<Cat> matchedCats = repository.getCatsNotMatchedYet(cat.raceCats, cat.health, cat.idCat); // II krok
        // todo opposite sex III krok
        log.info("matchedCats" + matchedCats);

        List<Integer> matchedCatsIds = getIds(matchedCats);
        for(int matchedCatId : matchedCatsIds) {
            //relationRepository.createRelationship(idCat, matchedCatId); II krok
        }

        List<Integer> matchedCatsOwnersIds = getOwnersIds(matchedCats);

        //todo get list of owner ids
        userService.createChats(cat.ownerid, matchedCatsOwnersIds);




        return matchedCats;
    }

    private List<Integer> getOwnersIds(List<Cat> matchedCats) {
        return List.of();
    }

    //dodaj get userid
    private List<Integer> getIds(List<Cat> matchedCats) {
        List<Integer> matchedCatsId = new ArrayList<>();
        for( Cat matchedCat  : matchedCats){
            matchedCatsId.add(matchedCat.idCat);
        }

        return matchedCatsId;
    }

}


