package pl.viksi.catsmatch.cat.api;

import pl.viksi.catsmatch.cat.domain.Health;

public class ChangCatNameRequest {

    String ciciuchName;
    String ciciuchRace;
    Health health;


    ChangCatNameRequest() {

    }
    @Override
    public String toString() {
        return String.format("Chat text = %s and text =%s ",  ciciuchName, ciciuchRace);
    }

    public void setCiciuchName(String ciciuchName) {
        this.ciciuchName = ciciuchName;
    }

    public void setCiciuchRace(String ciciuchRace) {
        this.ciciuchRace = ciciuchRace;
    }


    public void setHealth(Health health) {
        this.health = health;
    }

    }
