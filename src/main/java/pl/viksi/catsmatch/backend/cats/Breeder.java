package pl.viksi.catsmatch.backend.cats;
import jakarta.persistence.*;

@Entity @Table(name="mc_breeders")
public class Breeder {
    @Id public Integer id;
    @Column(nullable=false, length=120) public String kennel;
    @Column(nullable=false, length=100) public String city;
    @Column(nullable=false, length=100) public String country;
    @Column(nullable=false, length=2000) public String bio;
    protected Breeder() {}
    public Breeder(Integer id) { this.id=id; }
}
