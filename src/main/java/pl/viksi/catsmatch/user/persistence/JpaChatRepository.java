package pl.viksi.catsmatch.user.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.viksi.catsmatch.user.domain.Chat;

import java.util.List;

public interface JpaChatRepository extends JpaRepository<Chat, Integer> {

    List<Chat> findByUser1IDOrUser2ID(int user1ID, int user2ID);

}
