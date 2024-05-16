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

    String name;
    String raceCats;
    int idCat;

    Cat(String name, String raceCats, int idCat) {
        this.name = name;
        this.raceCats = raceCats;
        this.idCat = idCat;

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


}
