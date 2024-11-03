package pl.viksi.catsmatch.cat.api;

public class AddOwner {
    ;
    String name;

    public void setName(String name){
        this.name = name;
    }


    AddOwner(){

    }


    @Override
    public String toString() {
        return String.format("AddOwner name = %s", name);
    }

    public String getName() {
        return name;
    }

}
