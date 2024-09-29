package pl.viksi.catsmatch.account.api;

import pl.viksi.catsmatch.account.domain.Health;

public class ChangCatNameRequest {

    String ciciuchName;
    String ciciuchRace;
    int idCat;
    Health health;

    ChangCatNameRequest() {

    }

    public void setCiciuchName(String ciciuchName) {
        this.ciciuchName = ciciuchName;
    }

    public void setCiciuchRace(String ciciuchRace) {
        this.ciciuchRace = ciciuchRace;
    }

    public void setIdCat(int idCat) {
        this.idCat = idCat;
    }

    public void setHealth(Health health) {
        this.health = health;
    }
}
