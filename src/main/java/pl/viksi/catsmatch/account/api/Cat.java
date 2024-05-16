package pl.viksi.catsmatch.account.api;

public class Cat {

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


    String name;
    String raceCats;
    int idCat;
    Health health;

    Cat(String name, String raceCats, int idCat, Health health) {
        this.name = name;
        this.raceCats = raceCats;
        this.idCat = idCat;
        this.health = health;

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
}
