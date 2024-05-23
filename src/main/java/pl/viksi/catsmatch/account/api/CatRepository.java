package pl.viksi.catsmatch.account.api;

import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

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

        String InsertPositionSql = "Insert into cat values (?, ?, ?, ?);";
        try (
                PreparedStatement pstmt = con.prepareStatement(InsertPositionSql)) {
            pstmt.setString(1, cat.name);
            pstmt.setString(2,cat.raceCats);
            pstmt.setInt(3,cat.idCat);
            pstmt.setString(4, String.valueOf(cat.health));
            pstmt.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
