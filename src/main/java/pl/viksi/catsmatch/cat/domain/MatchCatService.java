package pl.viksi.catsmatch.cat.domain;

import pl.viksi.catsmatch.cat.persistence.CatRepository;
import pl.viksi.catsmatch.cat.persistence.RelationShipCatsRepository;
import pl.viksi.catsmatch.user.domain.UserService;

import java.util.ArrayList;
import java.util.List;

public class MatchCatService {


    CatRepository repository = new CatRepository();
    RelationShipCatsRepository relationRepository = new RelationShipCatsRepository();
    UserService userService = new UserService();

    public List<Cat> getMatchedAndGetMatches(int id) {


        Cat cat = repository.getCat(id);
        List<Cat> matchedCats = repository.getCats(cat.raceCats, cat.health);

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


