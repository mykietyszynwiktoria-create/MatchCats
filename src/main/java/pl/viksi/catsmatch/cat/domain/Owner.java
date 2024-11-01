package pl.viksi.catsmatch.cat.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name="usercat")
public class Owner {

    @GenericGenerator(name = "generator", strategy = "increment")
    @Id
    @GeneratedValue( generator = "generator")

    @Column(name = "usercatid")
    public Integer usercatid;

    @Column(name = "freesubscriptions")
    public int freesubscriptions;

    @Column(name = "counterforfreesub")
    public int counterforfreesub;

    @Column(name = "name1")
    public String name1;


    public int getFreesubscriptions() {
        return freesubscriptions;
    }

    public int getcounterforfreesub() {
        return counterforfreesub;
    }

    public String getName1() {
        return name1;
    }

    public int getUsercatid() {
        return usercatid;
    }

    public Owner() {

    }

    public Owner(int usercatid, int freesubscriptions, int counterforfreesub, String name1) {
        this.usercatid = usercatid;
        this.freesubscriptions = freesubscriptions;
        this.counterforfreesub = counterforfreesub;
        this.name1 = name1;

    }

    @Override
    public String toString() {
        return String.format("Owner with id = %d and with name1 = %s", usercatid, name1);

    }
    public int setFreesubscriptions(int freesubscriptions) {
        return freesubscriptions;
    }

    public int setcounterforfreesub(int counterforfreesub) {
        return counterforfreesub;
    }

    public String setName1(String name1) {
        return name1;
    }

    public int setUsercatid(int usercatid) {
        return usercatid;
    }

}
