package pl.viksi.catsmatch.backend.account;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="mc_email_verifications")
public class EmailVerificationToken {
    @Id @Column(name="token_hash",length=64) public String tokenHash;
    @Column(name="account_id",nullable=false,unique=true) public Integer accountId;
    @Column(nullable=false,length=254) public String email;
    @Column(name="expires_at",nullable=false) public Instant expiresAt;
    @Column(name="issued_at",nullable=false) public Instant issuedAt;
    protected EmailVerificationToken() {}
    public EmailVerificationToken(String hash,Account account,Instant issued) {
        tokenHash=hash;accountId=account.id;email=account.email;
        issuedAt=issued;expiresAt=issued.plusSeconds(86400);
    }
}
