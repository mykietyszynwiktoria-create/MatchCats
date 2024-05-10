package pl.viksi.catsmatch.account.api;

import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;


@RestController
public class AccountController {

    List<String> nameCats = new ArrayList<>();

    @GetMapping("/cats")
    public List<String> generate(){
        return nameCats;
    }

    @PostMapping("/cats")
    public void addCat(@RequestBody ChangCatNameRequest changCatNameRequest) {
        nameCats.add(changCatNameRequest.ciciuchName);
    }

    @DeleteMapping("/cats")
    public void deleteCat(@RequestBody ChangCatNameRequest changCatNameRequest) {
        nameCats.remove(changCatNameRequest.ciciuchName);
        System.out.println(nameCats);
    }

  }

