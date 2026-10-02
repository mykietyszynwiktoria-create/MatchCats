package pl.viksi.catsmatch.backend.cats;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity @Table(name="mc_cats")
public class Cat {
    public enum Sex { MALE, FEMALE }
    public enum Health { UNKNOWN, HEALTHY, SICK }
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Integer id;
    @Column(name="owner_id", nullable=false) public Integer ownerId;
    @Column(nullable=false, length=100) public String name;
    @Column(nullable=false, length=100) public String breed;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=10) public Sex sex;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=10) public Health health;
    @Column(name="birth_date", nullable=false) public LocalDate birthDate;
    @Column(nullable=false, length=100) public String city;
    @Column(nullable=false, length=100) public String country;
    @Column(nullable=false, length=2000) public String description;
    @Column(nullable=false) public boolean available;
    @Version @Column(nullable=false) public long version;
    protected Cat() {}
    public Cat(Integer ownerId) { this.ownerId=ownerId; }
}
