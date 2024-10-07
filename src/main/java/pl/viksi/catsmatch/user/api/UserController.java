package pl.viksi.catsmatch.user.api;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import pl.viksi.catsmatch.user.domain.Chat;
import pl.viksi.catsmatch.user.persistence.UserRepository;

public class UserController {
    Chat chats;

    UserRepository repository = new UserRepository();

    @PostMapping("/users")
    public int addUser(@PathVariable int userId) {
        return userId ;
    }

    @GetMapping("/users/{id}/chats")
    public Chat chats (String chats){
       return Chat.valueOf(chats);
    }

    @PostMapping("/users/{id}/chats/{id}/chat")
    public Chat addChat (){
        return null;
    }

}
