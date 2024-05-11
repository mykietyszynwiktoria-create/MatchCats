package pl.viksi.catsmatch.account.api;

import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;


@RestController
public class AccountController {

    List<Cat> nameCats = new ArrayList<Cat>();


    @GetMapping("/cats")
    public List<Cat> generate() {
        return nameCats;
    }

    @PostMapping("/cats")
    public void addCat(@RequestBody ChangCatNameRequest changCatNameRequest) {
        Cat newCat = new Cat(changCatNameRequest.ciciuchName, changCatNameRequest.ciciuchRace);
        nameCats.add(newCat);

    }

    @DeleteMapping("/cats")
    public void deleteCat(@RequestBody ChangCatNameRequest changCatNameRequest) {
        nameCats.remove(changCatNameRequest.ciciuchName);
        System.out.println(nameCats);

    }

}

