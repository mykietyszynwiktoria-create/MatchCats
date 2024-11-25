package pl.viksi.catsmatch.user.api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import pl.viksi.catsmatch.user.domain.*;
import pl.viksi.catsmatch.user.persistence.ChatRepository;
import pl.viksi.catsmatch.user.persistence.UsersRepository;

import java.util.List;

import static pl.viksi.catsmatch.cat.persistence.CatRepository.log;

@RestController
public class UserController {

    @Autowired
    UsersRepository repositoryUser;

    @Autowired
    ChatRepository repositoryChat;


    @PostMapping("/users")
    public PrivateUser addUser(@RequestBody CreatUser creatUser) {
        PrivateUser newUser = new PrivateUser(creatUser.nick_login, creatUser.login_password, creatUser.firstname,
                creatUser.surname, creatUser.email);

        log.info("Check new User" + newUser);

        return repositoryUser.saveUser(newUser);
    }


    @GetMapping("/users/{userId}/chats")
    public List<Chat> chats (@PathVariable int userId){

        log.info("check chats " + userId);

        return repositoryChat.getChats(userId);

    }

    @PostMapping("/users/chats/{idchat}")
    public Chat addMessageToChat(@RequestBody Message message, @PathVariable int idchat){

        log.info("Check message and idchat" + message + idchat);

        Chat chat = repositoryChat.findChat(idchat);

        log.info("message" + message);

         chat.addMessage(message);

         log.info("saveChat= " + chat);

        return repositoryChat.saveChat(chat);
    }

}
