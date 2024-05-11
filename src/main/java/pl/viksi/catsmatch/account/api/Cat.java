package pl.viksi.catsmatch.account.api;

public class Cat {

    public String getNameCats() {
        return nameCats;
    }

    public String getRaceCats() {
        return raceCats;
    }

    String nameCats;
    String raceCats;

    Cat(String nameCats, String raceCats) {
        this.nameCats = nameCats;
        this.raceCats = raceCats;
    }

    public void setRaceCats(String raceCats) {
        this.raceCats = raceCats;
    }

    public void setNameCats(String nameCats) {
        this.nameCats = nameCats;
    }


}
