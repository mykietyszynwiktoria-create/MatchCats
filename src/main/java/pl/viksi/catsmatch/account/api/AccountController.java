package pl.viksi.catsmatch.account.api;

import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;


@RestController
public class AccountController {

    List<Cat> cats = new ArrayList<Cat>();
    int sequenceNumber = 0;

    @GetMapping("/cats")
    public List<Cat> generate() {
        return cats;
    }

    @PostMapping("/cats")
    public int addCat(@RequestBody ChangCatNameRequest changCatNameRequest) {
        Cat newCat = new Cat(changCatNameRequest.ciciuchName, changCatNameRequest.ciciuchRace, changCatNameRequest.idCat);
        cats.add(newCat);
        int idCat = sequenceNumber;
        newCat.idCat = sequenceNumber++;
        return idCat;
    }

    @DeleteMapping("/cats")
    public void deleteCat(@RequestBody ChangCatNameRequest changCatNameRequest) {
        for (Cat cat : cats) {
            if (cat.idCat == (changCatNameRequest.idCat)) {
                cats.remove(cat);
                System.out.println(cats);
                break;
            }
        }
    }

}

