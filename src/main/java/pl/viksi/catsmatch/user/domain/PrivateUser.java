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



    @Override
    public String toString() {
        return String.format("Owner with id = %d and with nick_login = %s and with login_password = %s " +
                "and with firstname = %s and with surname = %s and with email = %s ", id, nick_login, login_password,
                firstname, surname, email);

    }
    public int setId(int id) {
        return id;
    }

    public String setNick_Login(String nick_login) {
        return nick_login;
    }

    public String setLogin_Password(String login_password) {
        return login_password;
    }

    public String setFirstname(String firstname) {
        return firstname;
    }

    public String setSurname(String surname){return surname;}

    public String setEmail(String email){return email;}



}
