package pl.viksi.catsmatch.user.domain;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.viksi.catsmatch.user.persistence.JpaUserRepository;

import java.util.List;

@Component
public class UserService {

    @Autowired
    JpaUserRepository jpaUserRepository;

    public void createChats(int userId, List<Integer> usersMatchedId) {


    }


    public boolean existUser(int userId) {

        PrivateUser findByIdUserId = jpaUserRepository.findById(userId);

        return findByIdUserId != null;
    }

}
