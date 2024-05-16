package pl.viksi.catsmatch.account.api;

public class ChangCatNameRequest {

    String ciciuchName;
    String ciciuchRace;
    int idCat;

    ChangCatNameRequest() {

    }

    public void setCiciuchName(String ciciuchName) {
        this.ciciuchName = ciciuchName;
    }

    public void setCiciuchRace(String ciciuchRace) {
        this.ciciuchRace = ciciuchRace;
    }

    public void setIdCat(int idCat ) {
        this.idCat = idCat;
    }

}
