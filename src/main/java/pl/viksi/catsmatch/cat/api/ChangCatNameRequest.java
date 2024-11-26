package pl.viksi.catsmatch.cat.api;

import jakarta.validation.constraints.NotNull;
import pl.viksi.catsmatch.cat.domain.Health;
import pl.viksi.catsmatch.cat.domain.Sex;

public class ChangCatNameRequest {

    @NotNull
    Sex sex;
    String ciciuchName;
    String ciciuchRace;
    Health health;
    int ownerid;

    ChangCatNameRequest() {

    }

    @Override
    public String toString() {
        return String.format("Chat text = %s and text =%s and text =%s", ciciuchName, ciciuchRace, ownerid);
    }

    public void setCiciuchName(String ciciuchName) {

        this.ciciuchName = ciciuchName;
    }

    public void setCiciuchRace(String ciciuchRace) {
        this.ciciuchRace = ciciuchRace;
    }

    public void setOwnerId(int ownerid) {
        this.ownerid = ownerid;
    }


    public void setHealth(Health health) {
        this.health = health;
    }

    public void setSex(Sex sex){this.sex = sex;}
}
