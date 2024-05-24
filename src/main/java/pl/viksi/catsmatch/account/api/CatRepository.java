package pl.viksi.catsmatch.account.api;

import org.springframework.stereotype.Repository;

import java.sql.*;

public class CatRepository {

    Connection con;

    CatRepository() {

        try {
            con = DriverManager
                    .getConnection("jdbc:postgresql://localhost:5432/mycatmatch", "mycatmatch", "mysweetcat");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    public void saveCat( Cat cat){

        String InsertPositionSql = "Insert into cat(catname, catcolor, catID," +
                " health) values (?, ?, nextval('serialCat'), ?);";
        try (
                PreparedStatement pstmt = con.prepareStatement(InsertPositionSql)) {
            int primkey = 0;

            pstmt.setString(1, cat.name);
            pstmt.setString(2,cat.raceCats);
            pstmt.setString(3, String.valueOf(cat.health));
            pstmt.execute();
            {
                ResultSet generatedKeys = pstmt.getGeneratedKeys();
                if (generatedKeys.next())
                    primkey = generatedKeys.getInt(1);}
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
