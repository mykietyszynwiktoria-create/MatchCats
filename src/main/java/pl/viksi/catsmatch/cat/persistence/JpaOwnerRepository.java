package pl.viksi.catsmatch.cat.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.viksi.catsmatch.cat.domain.Cat;
import pl.viksi.catsmatch.cat.domain.Owner;

import java.util.List;

public interface JpaOwnerRepository extends JpaRepository<Owner, Integer> {

    Owner findById(int usercatid);

}
