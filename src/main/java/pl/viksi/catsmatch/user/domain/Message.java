package pl.viksi.catsmatch.user.domain;

public class Message {
    String text;

    @Override
    public String toString() {
        return String.format("Message text = %s", text);
    }

    public void setText(String text) {
        this.text = text;
    }
}
