package pl.viksi.catsmatch.account.api;

import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class CatRepository {

    CatRepository() {

        try (Connection con = DriverManager
                .getConnection("jdbc:postgresql://localhost:5432/mycatmatch", "mycatmatch", "mysweetcat")) {

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    ;
}
