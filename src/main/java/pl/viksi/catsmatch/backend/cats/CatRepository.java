package pl.viksi.catsmatch.backend.cats;
import org.springframework.data.jpa.repository.*;
public interface CatRepository extends JpaRepository<Cat,Integer>, JpaSpecificationExecutor<Cat> {}
