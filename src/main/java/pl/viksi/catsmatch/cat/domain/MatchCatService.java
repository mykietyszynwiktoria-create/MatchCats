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

    public List<Cat> getMatchedAndGetMatches(int id) {

        log.info("Getting matchedCats with catId: " + id);
        Cat cat = repository.getCat(id);

        log.info("cat returned: " + cat);
        List<Cat> matchedCats = repository.getCats(cat.raceCats, cat.health);

        log.info("matchedCats" + matchedCats);
        List<Integer> matchedCatsIds = getIds(matchedCats);
        for(int matchedCatId : matchedCatsIds) {
            relationRepository.createRelationship(id,matchedCatId);
        }

        userService.informCatsMatched(cat.idCat, matchedCatsIds);




        return matchedCats;
    }

    private List<Integer> getIds(List<Cat> matchedCats) {
        List<Integer> matchedCatsId = new ArrayList<>();
        for( Cat matchedCat  : matchedCats){
            matchedCatsId.add(matchedCat.idCat);
        }

        return matchedCatsId;
    }

}


