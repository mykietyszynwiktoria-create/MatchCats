package pl.viksi.catsmatch.user.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import pl.viksi.catsmatch.user.domain.Chat;
import pl.viksi.catsmatch.user.domain.CreatUser;
import pl.viksi.catsmatch.user.domain.Message;
import pl.viksi.catsmatch.user.domain.PrivateUser;
import pl.viksi.catsmatch.user.persistence.UsersRepository;

import java.util.List;

@RestController
public class UserController {
    Chat chats;

    @Autowired
    UsersRepository repository = new UsersRepository();

    @PostMapping("/users")
    public PrivateUser addUser(@RequestBody CreatUser creatUser) {
        PrivateUser newUser = new PrivateUser(creatUser.nick_login, creatUser.login_password, creatUser.firstname,
                creatUser.surname, creatUser.email);
        return repository.saveUser(newUser);
    }


    @GetMapping("/users/{id}/chats")
    public List<Chat> chats (@PathVariable int userId){
       return null;
    }

    @PostMapping("/users/{iduser}/chats/{idchat}/chat")
    public Chat addChat (@RequestBody Message message, @PathVariable int iduser, @PathVariable int idchat){
        return null;
    }

}
