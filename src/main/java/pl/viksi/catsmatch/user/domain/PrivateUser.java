package pl.viksi.catsmatch.user.domain;
import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name="users_users")
public class PrivateUser {

    @GenericGenerator(name = "generator2", strategy = "increment")
    @Id
    @GeneratedValue( generator = "generator2")
    @Column(name = "id")
    public int id;
    @Column(name = "nick_login")
    public String nick_login;
    @Column(name = "login_password")
    public String login_password;
    @Column(name = "firstname")
    public String firstname;
    @Column(name = "surname")
    public String surname;
    @Column(name = "email")
    public String email;

    public PrivateUser(String nick_login, String login_password, String firstname, String surname, String email) {
    this.nick_login = nick_login;
    this.login_password = login_password;
    this.firstname = firstname;
    this.surname = surname;
    this.email = email;

    }

    public PrivateUser(){

    }

    public void setId(int id){
        this.id = id;
    }

    public void setNick_login(String nick_login) {
        this.nick_login = nick_login;
    }

    public void setLogin_password(String login_password) {
       this.login_password = login_password;
    }


    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public void setEmail(String email) {
        this.email = email;
    }

}
