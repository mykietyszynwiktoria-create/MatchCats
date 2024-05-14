package pl.viksi.catsmatch.account.api;

public class Cat {

    public String getNameCats() {
        return nameCats;
    }

    public String getRaceCats() {
        return raceCats;
    }

    public int getIdCat() {
        return idCat;
    }

    String nameCats;
    String raceCats;
    int idCat;

    Cat(String nameCats, String raceCats, int idCat) {
        this.nameCats = nameCats;
        this.raceCats = raceCats;
        this.idCat = idCat;

    }

    public void setRaceCats(String raceCats) {
        this.raceCats = raceCats;
    }

    public void setNameCats(String nameCats) {
        this.nameCats = nameCats;
    }

    public void setIdCats(int idCat) {
        this.idCat = idCat;
    }


}
