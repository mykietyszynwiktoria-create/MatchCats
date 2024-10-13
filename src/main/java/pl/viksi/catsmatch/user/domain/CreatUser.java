package pl.viksi.catsmatch.user.domain;

public class CreatUser {
    public static Object creatUser;
    public int userId;
    public String nick_login;
    public String login_password;
    public String firstname;
    public String surname;
    public String email;


    public int getIdUser() {
        return userId;

    }

    public void setIdUser(int userId) {
        this.userId = userId;
    }

    public String getNick_login() {
        return nick_login;
    }

    public void setNick_login(String nick_login) {
        this.nick_login = nick_login;
    }

    public String getLogin_password() {
        return login_password;
    }

    public void setLogin_password(String login_password) {
        this.login_password = login_password;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
