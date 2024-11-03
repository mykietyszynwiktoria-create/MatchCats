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
    public String name;

    public int getFreesubscriptions() {
        return freesubscriptions;
    }

    public int getcounterforfreesub() {
        return counterforfreesub;
    }

    public String getName() {
        return name;
    }

    public int getUsercatid() {
        return usercatid;
    }

    public Owner() {

    }

    public Owner(int usercatid, int freesubscriptions, int counterforfreesub, String name) {
        this.usercatid = usercatid;
        this.freesubscriptions = freesubscriptions;
        this.counterforfreesub = counterforfreesub;
        this.name = name;

    }

    @Override
    public String toString() {
        return String.format("Owner with id = %d and with name = %s", usercatid, name);

    }
    public int setFreesubscriptions(int freesubscriptions) {
        return freesubscriptions;
    }

    public int setCounterforfreesub(int counterforfreesub) {
        return counterforfreesub;
    }

    public String setName(String name) {
        return name;
    }

    public int setUsercatid(int usercatid) {
        return usercatid;
    }

}
