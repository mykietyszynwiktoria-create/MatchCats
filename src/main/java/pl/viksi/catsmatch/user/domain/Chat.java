package pl.viksi.catsmatch.user.domain;
import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;
import pl.viksi.catsmatch.cat.domain.Health;

import static pl.viksi.catsmatch.cat.persistence.CatRepository.log;

@Entity
@Table(name="users_chats")
public class Chat {

    @GenericGenerator(name = "generator3", strategy = "increment")
    @Id
    @GeneratedValue( generator = "generator3")
    @Column(name = "chat_informationID")
    public int id;
    @Column(name = "user1ID")
    public int user1ID;
    @Column(name = "user2ID")
    public int user2ID;
    @Column(name = "chat_cats")
    public String text;

    public Chat(){

    }

    public Chat(int id, int user1ID, int user2ID, String text) {
        log.info("check id" + id);

        this.id = id;
        log.info("check user1ID" + user1ID );

        this.user1ID = user1ID;
        log.info("check user2ID" + user2ID);

        this.user2ID = user2ID;
        log.info("check text " + text);

        this.text= text;

    }

    public void addMessage(Message message){

     log.info("check message" + text);

     this.text = this.text + " " + message.text;

    }

    @Override
    public String toString() {
        return String.format("Chat with id = %d and with text = %s", id, text);
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setUser1ID(int user1ID) {
        this.user1ID = user1ID;
    }

    public void setUser2ID(int user2ID) {
        this.user2ID = user2ID;
    }

    public void setText(String text) {
        this.text = text;
    }
}

