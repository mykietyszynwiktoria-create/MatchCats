package pl.viksi.catsmatch.cat.persistence;

import org.springframework.stereotype.Component;
import pl.viksi.catsmatch.cat.domain.Owner;

import java.sql.*;

import static pl.viksi.catsmatch.cat.persistence.CatRepository.log;

@Component
public class OwnerRepository {

    Connection con;

    public OwnerRepository() {

        try {
            con = DriverManager
                    .getConnection("jdbc:postgresql://localhost:5432/mycatmatch", "mycatmatch", "mysweetcat");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    public Owner insertOwner(Owner owner) {

        log.info("Check InsertPositionSql name1" + owner.name);

        String InsertPositionSql = "Insert into usercat(usercatid, freesubscriptions, counterforfreesub, name1)" +
                " values (?, ?, ?, ?);";
        try (
                PreparedStatement pstmt = con.prepareStatement(InsertPositionSql)) {
            int primkey = 0;

            pstmt.setInt(1, owner.usercatid);
            pstmt.setInt(2, owner.freesubscriptions);
            pstmt.setInt(3, owner.counterforfreesub);
            pstmt.setString(4, owner.name);

            pstmt.execute();
            {
                ResultSet generatedKeys = pstmt.getGeneratedKeys();
                if (generatedKeys.next())
                    primkey = generatedKeys.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public Owner findOwner(int ownerid) throws SQLException {

        PreparedStatement pstmt = con.prepareStatement("SELECT * FROM usercat WHERE usercatid = ?");
        pstmt.setInt(1, ownerid);
        ResultSet resultSet = pstmt.executeQuery();
        if (resultSet.next()) {
            int usercatid = resultSet.getInt("usercatid");
            int freesubscriptions = resultSet.getInt("freesubscriptions");
            int counterforfreesub = resultSet.getInt("counterforfreesub");
            String name1 = resultSet.getString("name1");
                    return new Owner(usercatid, freesubscriptions, counterforfreesub, name1);

        }
        return null;
    }

}