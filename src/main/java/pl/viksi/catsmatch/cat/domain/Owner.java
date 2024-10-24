package pl.viksi.catsmatch.cat.domain;

public class Owner {

    public int freesubscriptions;
    public int counterforfreesub;
    public String name1;
    public int usercatid;

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

    public Owner(int freesubscriptions, int counterforfreesub, String name1, int usercatid) {

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
