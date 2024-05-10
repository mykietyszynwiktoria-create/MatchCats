package pl.viksi.catsmatch.account.api;

import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@RestController
public class AccountController {

    String staryCiciuch= "Kapec";
    String nowyCiciuch = "Sara";
    String mediumCiciuch = "Mis";
    List<String> nameCats = new ArrayList<>();

    @GetMapping("/helloword")
    public String helloWord(){
        System.out.println("Hello word");

       return "Hello word";
    }

    @GetMapping("/danyciciuch")
    public String danyCiciuch(){
            return nowyCiciuch + mediumCiciuch + staryCiciuch;
    }

    @PutMapping("/danyciciuch")
    public void changCiciuchName(@RequestBody ChangCatNameRequest changCatNameRequest){
    staryCiciuch = mediumCiciuch;
    mediumCiciuch = nowyCiciuch;
    nowyCiciuch = changCatNameRequest.ciciuchName;
        System.out.println(changCatNameRequest.ciciuchName);

    }

    @GetMapping("/cats")
    public List<String> generate(){
        return nameCats;
    }

    @PostMapping("/cats")
    public void addCat(@RequestBody ChangCatNameRequest changCatNameRequest) {
        nameCats = new ArrayList<>();
        nameCats.add(changCatNameRequest.ciciuchName);
    }

}

