package pl.viksi.catsmatch.cat.persistence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.viksi.catsmatch.cat.domain.Owner;

import java.sql.*;

public class OwnerRepository {

    @Autowired
    JpaOwnerRepository jpaOwnerRepository;

    Connection con;

    public OwnerRepository() {

        try {
            con = DriverManager
                    .getConnection("jdbc:postgresql://localhost:5432/mycatmatch", "mycatmatch", "mysweetcat");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    public void AddOwner(Owner owner) {

        String InsertPositionSql = "Insert into usercat(freesubscriptions, counterforfreesub, name1, usercatid )" +
                " values (?, ?, ?, ?);";
        try (
                PreparedStatement pstmt = con.prepareStatement(InsertPositionSql)) {
            int primkey = 0;

            pstmt.setInt(1, owner.freesubscriptions);
            pstmt.setInt(2, owner.counterforfreesub);
            pstmt.setString(3, owner.name1);
            pstmt.setInt(4, owner.usercatid);
            pstmt.execute();
            {
                ResultSet generatedKeys = pstmt.getGeneratedKeys();
                if (generatedKeys.next())
                    primkey = generatedKeys.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

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

    public Owner saveOwner(Owner owner){

        return  jpaOwnerRepository.save(owner);
    }
}