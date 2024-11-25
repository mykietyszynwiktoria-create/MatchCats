package pl.viksi.catsmatch.user.domain;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;import pl.viksi.catsmatch.user.persistence.ChatRepository;
import pl.viksi.catsmatch.user.persistence.JpaUserRepository;

import java.util.List;

@Component
public class UserService {

    @Autowired
    ChatRepository repositoryChat;

    @Autowired
    JpaUserRepository jpaUserRepository;

    public void createChats(int userId, List<Integer> usersMatchedId) {
        for (Integer userMatchedId : usersMatchedId) {

            if (!repositoryChat.exsistChat(userId,userMatchedId) && !repositoryChat.exsistChat(userMatchedId,userId)){
                Chat createChat = new Chat(userId, userMatchedId, " ");
                repositoryChat.saveChat(createChat);
            }

        }

    }


    public boolean existUser(int userId) {

        PrivateUser findByIdUserId = jpaUserRepository.findById(userId);

        return findByIdUserId != null;
    }

}
