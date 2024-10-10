package pl.viksi.catsmatch.user.persistence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.viksi.catsmatch.user.domain.User;


@Component
public class UsersRepository {

    @Autowired
    JpaUserRepository jpaRelationshipUserRepository;

    public void createUser(User user) {

        jpaRelationshipUserRepository.save(user);

    }
}
