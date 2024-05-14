package pl.viksi.catsmatch.account.api;

import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;


@RestController
public class AccountController {

    List<Cat> nameCats = new ArrayList<Cat>();
    int sequenceNumber = 0;

    @GetMapping("/cats")
    public List<Cat> generate() {
        return nameCats;
    }

    @PostMapping("/cats")
    public int addCat(@RequestBody ChangCatNameRequest changCatNameRequest) {
        Cat newCat = new Cat(changCatNameRequest.ciciuchName, changCatNameRequest.ciciuchRace,changCatNameRequest.idCat  );
        nameCats.add(newCat);
        int idCat = sequenceNumber;
        newCat.idCat = sequenceNumber++;
        return idCat;
    }

    @DeleteMapping("/cats")
    public void deleteCat(@RequestBody ChangCatNameRequest changCatNameRequest) {
        for (Cat cat : nameCats){
            if (cat.nameCats.equals(changCatNameRequest.ciciuchName)) {
                nameCats.remove(cat);
                System.out.println(nameCats);
                break;
            }
        }
    }

}

