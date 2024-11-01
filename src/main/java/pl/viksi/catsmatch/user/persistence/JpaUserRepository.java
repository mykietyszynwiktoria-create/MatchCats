package pl.viksi.catsmatch.user.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.viksi.catsmatch.user.domain.PrivateUser;

public interface JpaUserRepository extends JpaRepository<PrivateUser, Integer> {
    PrivateUser findById(int id);
}

