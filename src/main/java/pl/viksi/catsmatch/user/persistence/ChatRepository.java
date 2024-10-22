package pl.viksi.catsmatch.user.persistence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.viksi.catsmatch.user.domain.Chat;
import pl.viksi.catsmatch.user.domain.Message;

import java.util.List;

@Component
public class ChatRepository {

    @Autowired
    JpaChatRepository jpaChatRepository;

    public Chat saveChat(Chat chat){
        return jpaChatRepository.save(chat);
    }

    public List<Chat> getChats(int idUser){
        return jpaChatRepository.findByUser1IDOrUser2ID(idUser, idUser);
    }

    public Chat findChat(int idchat){
        return jpaChatRepository.findById(idchat);
    }
}
