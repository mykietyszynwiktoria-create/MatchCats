package pl.viksi.catsmatch.backend.account;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerificationToken,String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from EmailVerificationToken t where t.tokenHash = :hash")
    Optional<EmailVerificationToken> locked(@Param("hash") String hash);
    Optional<EmailVerificationToken> findByAccountId(Integer accountId);
    void deleteAllByAccountId(Integer accountId);
}
