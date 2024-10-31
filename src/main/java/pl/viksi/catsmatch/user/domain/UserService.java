package pl.viksi.catsmatch.user.domain;

import org.springframework.beans.factory.annotation.Autowired;
import pl.viksi.catsmatch.user.persistence.ChatRepository;
import pl.viksi.catsmatch.user.persistence.JpaUserRepository;
import pl.viksi.catsmatch.user.persistence.UsersRepository;

import java.util.List;

public class UserService {

    @Autowired
    JpaUserRepository jpaUserRepository;

    public void informCatsMatched(int idCat, List<Integer> matchedCatsIds) {
    }


    public boolean existUser(int userId) {
        return jpaUserRepository.findById(userId) != null;
    }
}
