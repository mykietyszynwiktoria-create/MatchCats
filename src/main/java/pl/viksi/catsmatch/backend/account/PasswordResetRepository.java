package pl.viksi.catsmatch.backend.account;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface PasswordResetRepository extends JpaRepository<PasswordResetToken,String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from PasswordResetToken t where t.tokenHash = :hash")
    Optional<PasswordResetToken> locked(@Param("hash") String hash);
    Optional<PasswordResetToken> findByAccountId(Integer accountId);
    void deleteAllByAccountId(Integer accountId);
}
