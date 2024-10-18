package pl.viksi.catsmatch.user.domain;
import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;

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
        this.id = id;
        this.user1ID = user1ID;
        this.user2ID = user2ID;
        this.text= text;

    }

    public void addMessage(Message message){


    }

}
