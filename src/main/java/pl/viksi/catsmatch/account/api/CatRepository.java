package pl.viksi.catsmatch.account.api;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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

    public void saveCat(Cat cat) {

        String InsertPositionSql = "Insert into cat(catname, catcolor, catID," +
                " health) values (?, ?, nextval('serialCat'), ?);";
        try (
                PreparedStatement pstmt = con.prepareStatement(InsertPositionSql)) {
            int primkey = 0;

            pstmt.setString(1, cat.name);
            pstmt.setString(2, cat.raceCats);
            pstmt.setString(3, String.valueOf(cat.health));
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

    public List<Cat> getCats() {
        List<Cat> catList;
        String query = "SELECT * FROM cat";
        try (Statement stmt = con.createStatement();
             ResultSet resultSet = stmt.executeQuery(query)) {
             catList = new ArrayList<>();
            while (resultSet.next()) {
                int catId = resultSet.getInt("catId");
                String catname = resultSet.getString("catname");
                String catcolor = resultSet.getString("catcolor");
                Health health;
                if(resultSet.getString("health") == null )
                {
                   health = null;

                }
                else{

                    health = Health.valueOf(resultSet.getString("health"));
                }
                Cat cat = new Cat(catname, catcolor, catId, health);
                catList.add(cat);

            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
      return catList;
    }

    public void deleteCat(int idCat)
    {   
        String query = "DELETE FROM cat WHERE catId = ?";
                    try (
             PreparedStatement pstmt = con.prepareStatement(query)) {

            pstmt.setInt(1, idCat);
            pstmt.execute();
            {


        }
    } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

}