package pl.viksi.catsmatch.user.persistence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.viksi.catsmatch.user.domain.PrivateUser;

@Component
public class UsersRepository {

    @Autowired
    JpaUserRepository jpaUserRepository;

    public void createUser(PrivateUser user) {
        var saveduser = jpaUserRepository.save(user);

        ;

    }
}
