package pl.viksi.catsmatch.account.api;

public class ChangCatNameRequest {

    String ciciuchName;
    String ciciuchRace;

    ChangCatNameRequest() {

    }

    public void setCiciuchName(String ciciuchName) {
        this.ciciuchName = ciciuchName;
    }

    public void setCiciuchRace(String ciciuchRace) {
        this.ciciuchRace = ciciuchRace;
    }

}
