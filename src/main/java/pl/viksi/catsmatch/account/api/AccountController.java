package pl.viksi.catsmatch.account.api;

import org.springframework.web.bind.annotation.*;


@RestController
public class AccountController {

    String danyCiciuch = "Kapec";
    String danyCiciuch2 = "Sara";

    @GetMapping("/helloword")
    public String helloWord(){
        System.out.println("Hello word");

       return "Hello word";
    }

    @GetMapping("/danyciciuch")
    public String danyCiciuch(){

        return danyCiciuch + danyCiciuch2;
    }

    @PutMapping("/danyciciuch")
    public void changCiciuchName(@RequestBody ChangCatNameRequest changCatNameRequest){
    danyCiciuch = changCatNameRequest.ciciuchName ;
        System.out.println(changCatNameRequest.ciciuchName);


    }

}
