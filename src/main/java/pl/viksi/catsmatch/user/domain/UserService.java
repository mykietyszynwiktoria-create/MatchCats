package pl.viksi.catsmatch.user.domain;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.viksi.catsmatch.user.persistence.ChatRepository;
import pl.viksi.catsmatch.user.persistence.JpaUserRepository;
import pl.viksi.catsmatch.user.persistence.UsersRepository;

import java.util.List;

import static pl.viksi.catsmatch.cat.persistence.CatRepository.log;

@Component
public class UserService {

    @Autowired
    JpaUserRepository jpaUserRepository;

    public void informCatsMatched(int idCat, List<Integer> matchedCatsIds) {
    }


    public boolean existUser(int userId) {

        PrivateUser findByIdUserId = jpaUserRepository.findById(userId);

        return findByIdUserId != null;
    }

}
