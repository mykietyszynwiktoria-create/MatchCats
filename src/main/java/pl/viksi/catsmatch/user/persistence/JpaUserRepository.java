package pl.viksi.catsmatch.user.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.viksi.catsmatch.user.domain.User;

public interface JpaUserRepository extends JpaRepository<User, Integer> {
}
