package pl.viksi.catsmatch.user.domain;
import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "users.users")
public class PrivateUser {

    @GenericGenerator(name = "generator", strategy = "increment")
    @Id
    @GeneratedValue( generator = "generator")
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

    public PrivateUser(int id, String nick_login, String login_password, String firstname, String surname, String email) {
    this.id = id;
    this.nick_login = nick_login;
    this.login_password = login_password;
    this.firstname = firstname;
    this.surname = surname;
    this.email = email;

    }
}
