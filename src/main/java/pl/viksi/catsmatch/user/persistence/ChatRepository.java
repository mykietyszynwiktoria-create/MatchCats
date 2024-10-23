package pl.viksi.catsmatch.user.persistence;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.viksi.catsmatch.cat.domain.MatchCatService;
import pl.viksi.catsmatch.user.domain.Chat;
import pl.viksi.catsmatch.user.domain.Message;

import java.util.List;

import static pl.viksi.catsmatch.cat.persistence.CatRepository.log;

@Component
public class ChatRepository {
    private static final Log log = LogFactory.getLog(ChatRepository.class);
    @Autowired
    JpaChatRepository jpaChatRepository;

    public Chat saveChat(Chat chat){

        log.info("Check saveChat" + chat);

        return jpaChatRepository.save(chat);

    }

    public List<Chat> getChats(int idUser){
        return jpaChatRepository.findByUser1IDOrUser2ID(idUser, idUser);
    }

    public Chat findChat(int idchat){
        return jpaChatRepository.findById(idchat);
    }
}
