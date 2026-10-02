package pl.viksi.catsmatch.backend.account;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "mc_accounts")
public class Account {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer id;
    @Column(nullable = false, unique = true, length = 40)
    public String username;
    @JsonIgnore @Column(name = "password_hash", nullable = false, length = 100)
    public String passwordHash;
    @Column(nullable = false, unique = true, length = 254)
    public String email;
    @Column(name = "first_name", nullable = false, length = 80)
    public String firstName;
    @Column(nullable = false, length = 80)
    public String surname;
    @JsonIgnore @Column(name="security_version", nullable=false)
    public long securityVersion;
    protected Account() {}
    public Account(String username, String hash, String email, String firstName, String surname) {
        this.username = username; this.passwordHash = hash; this.email = email;
        this.firstName = firstName; this.surname = surname;
    }
}
