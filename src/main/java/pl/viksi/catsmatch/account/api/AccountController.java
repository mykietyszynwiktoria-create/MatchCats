package pl.viksi.catsmatch.account.api;

import org.springframework.web.bind.annotation.*;


@RestController
public class AccountController {

    String staryCiciuch= "Kapec";
    String nowyCiciuch = "Sara";

    @GetMapping("/helloword")
    public String helloWord(){
        System.out.println("Hello word");

       return "Hello word";
    }

    @GetMapping("/danyciciuch")
    public String danyCiciuch(){
            return nowyCiciuch + staryCiciuch;
    }

    @PutMapping("/danyciciuch")
    public void changCiciuchName(@RequestBody ChangCatNameRequest changCatNameRequest){
    staryCiciuch = nowyCiciuch;
    nowyCiciuch = changCatNameRequest.ciciuchName;
        System.out.println(changCatNameRequest.ciciuchName);

    }

}
