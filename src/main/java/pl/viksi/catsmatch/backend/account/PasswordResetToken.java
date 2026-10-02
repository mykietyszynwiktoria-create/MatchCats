package pl.viksi.catsmatch.backend.account;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="mc_password_resets")
public class PasswordResetToken {
    @Id @Column(name="token_hash",length=64) public String tokenHash;
    @Column(name="account_id",nullable=false) public Integer accountId;
    @Column(name="expires_at",nullable=false) public Instant expiresAt;
    protected PasswordResetToken() {}
    public PasswordResetToken(String hash,int account,Instant expires){tokenHash=hash;accountId=account;expiresAt=expires;}
}
