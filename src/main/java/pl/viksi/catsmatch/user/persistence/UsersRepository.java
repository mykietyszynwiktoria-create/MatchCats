package pl.viksi.catsmatch.user.persistence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.viksi.catsmatch.user.domain.PrivateUser;

import java.util.Optional;

@Component
public class UsersRepository {

    @Autowired
    JpaUserRepository jpaUserRepository;



    public PrivateUser saveUser(PrivateUser user) {
        return jpaUserRepository.save(user);

    }

    public PrivateUser findUser(int userId){
        return jpaUserRepository.findById(userId);
    }
}
