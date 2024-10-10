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


    public String name;
    public String raceCats;
    public int idCat;
    public Health health;


    public Cat(String name, String raceCats, int idCat, Health health) {

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
}
