package pl.viksi.catsmatch.user.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.viksi.catsmatch.user.domain.Chat;

import java.util.List;

import static pl.viksi.catsmatch.cat.persistence.CatRepository.log;

public interface JpaChatRepository extends JpaRepository<Chat, Integer> {

    List<Chat> findByUser1IDOrUser2ID(int user1ID, int user2ID);

    Chat findById(int idchat);

    boolean existsByUser1IDAndUser2ID(int user1ID, int user2ID);

}
