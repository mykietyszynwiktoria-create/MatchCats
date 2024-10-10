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
}
