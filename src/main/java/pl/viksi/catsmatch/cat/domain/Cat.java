package pl.viksi.catsmatch.cat.domain;

public class Cat {

    public static void add(Cat newUser) {
    }

    // todo use getters
    public String getName() {
        return name;
    }

    public String getRaceCats() {
        return raceCats;
    }

    public int getIdCat() {
        return idCat;
    }

    public Health getHealth() {
        return health;
    }

    public Sex getSex(){
        return sex;
    }

    public int getOwnerid(){return ownerid;}


    public String name;
    public String raceCats;
    public int idCat;
    public Health health;
    public int ownerid;
    public Sex sex;

    public Cat(String name, String raceCats, Health health, Sex sex, int ownerid) {

        this.name = name;

        this.raceCats = raceCats;

        this.health = health;

        this.sex = sex;

        this.ownerid = ownerid;

    }

    public Cat(int idCat, String name, String raceCats, Health health, Sex sex, int ownerid) {

        this(name, raceCats, health, sex, ownerid);

        this.idCat = idCat;

    }
    @Override
    public String toString() {
        return String.format("Cat with id = %d and with name = %s", idCat, name);
    }

    public void setRaceCats(String raceCats) {
        this.raceCats = raceCats;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setIdCats(int idCat) {
        this.idCat = idCat;
    }

    public void setHealth(Health health) {
        this.health = health;
    }

    public void setSex(Sex sex) {
        this.sex = sex;
    }

    public void setOwnerid(int ownerid) { this.ownerid = ownerid;}

}
