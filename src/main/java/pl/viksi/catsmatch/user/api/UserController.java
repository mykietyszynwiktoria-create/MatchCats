package pl.viksi.catsmatch.user.api;
import org.apache.catalina.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import pl.viksi.catsmatch.user.domain.Chat;
import pl.viksi.catsmatch.user.domain.CreatUser;
import pl.viksi.catsmatch.user.domain.Message;

import java.util.List;

public class UserController {
    Chat chats;

    UserRepository repository = new UserRepository();

    @PostMapping("/users")
    public int addUser(@RequestBody CreatUser creatUser) {
        User newUser = new User(CreatUser.creatUser);
        User.add(newUser);
        repository.creatUser(newUser);
        return  ;
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
