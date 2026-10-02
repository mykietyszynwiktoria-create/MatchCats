package pl.viksi.catsmatch.backend.chat;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    Optional<Conversation> findByFirstOwnerIdAndSecondOwnerId(Integer first, Integer second);

    @Query("select c from Conversation c where c.firstOwnerId = :owner or c.secondOwnerId = :owner")
    Page<Conversation> belongingTo(@Param("owner") Integer owner, Pageable pageable);
}
