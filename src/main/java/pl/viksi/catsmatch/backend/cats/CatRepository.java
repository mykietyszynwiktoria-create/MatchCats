package pl.viksi.catsmatch.backend.cats;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;

public interface CatRepository extends JpaRepository<Cat,Integer>, JpaSpecificationExecutor<Cat> {
    @Query("select distinct c.ownerId from Cat c where c.id in :ids")
    List<Integer> ownerIds(@Param("ids") Collection<Integer> ids);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cat c where c.id in :ids order by c.id")
    List<Cat> lockCats(@Param("ids") Collection<Integer> ids);
}
