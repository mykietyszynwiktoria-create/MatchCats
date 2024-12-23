package pl.viksi.catsmatch.cat.persistence;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import pl.viksi.catsmatch.cat.domain.Cat;
import pl.viksi.catsmatch.cat.domain.CustomerException;
import pl.viksi.catsmatch.cat.domain.Health;
import pl.viksi.catsmatch.cat.domain.Sex;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CatRepository {

    public static final Log log = LogFactory.getLog(CatRepository.class);
    Connection con;

    public CatRepository() {

        try {
            con = DriverManager
                    .getConnection("jdbc:postgresql://localhost:5432/mycatmatch", "mycatmatch", "mysweetcat");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    public int saveCat(Cat cat) {

        String InsertPositionSql = "Insert into cat(catname, catcolor, userid, catID," +
                " health, sex) values (?, ?, ?, nextval('serialCat'), ?, ?);";
        int primkey;
        try (
                PreparedStatement pstmt = con.prepareStatement(InsertPositionSql, Statement.RETURN_GENERATED_KEYS)) {
            primkey = 0;

            pstmt.setString(1, cat.name);
            pstmt.setString(2, cat.raceCats);
            pstmt.setInt(3, cat.ownerid);
            pstmt.setString(4, String.valueOf(cat.health));
            pstmt.setString(5, String.valueOf(cat.sex));
            pstmt.execute();
            {
                ResultSet generatedKeys = pstmt.getGeneratedKeys();
                if (generatedKeys.next())
                    primkey = generatedKeys.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return primkey;
    }

    public List<Cat> getMatchedAndGetMatches(int id) {


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
                if (resultSet.getString("health") == null) {
                    health = null;

                } else {

                    health = Health.valueOf(resultSet.getString("health"));
                }
                Sex sex;
                if (resultSet.getString("sex") == null) {
                    sex = null;

                } else {

                    sex = Sex.valueOf(resultSet.getString("sex"));
                }
                int ownerid = resultSet.getInt("ownerid");
                Cat cat = new Cat(catId, catname, catcolor, health, sex, ownerid);
                catList.add(cat);

            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return catList;
    }

    public void deleteCat(int idCat) {
        String query1 = "DELETE FROM chat_information WHERE (user1id = ?) OR (user2id = ?)";
        try (
                PreparedStatement pstmt = con.prepareStatement(query1)) {

            pstmt.setInt(1, idCat);
            pstmt.setInt(2, idCat);

            pstmt.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }


        String query2 = "DELETE FROM cat WHERE catId = ?";
        try (
                PreparedStatement pstmt = con.prepareStatement(query2)) {

            pstmt.setInt(1, idCat);
            pstmt.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    public void upDateCat(int id, Health health) {

        String queryUpDate = "UPDATE cat SET health = ? WHERE catid = ?";
        try (
                PreparedStatement pstmt = con.prepareStatement(queryUpDate)) {

            pstmt.setString(1, String.valueOf(health));
            pstmt.setInt(2, id);

            pstmt.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    public Cat getCat(int catId) {

        String queryGetCat = "SELECT *  FROM cat WHERE catId = ?";
        try (
                PreparedStatement pstmt = con.prepareStatement(queryGetCat)) {

            pstmt.setInt(1, catId);

            ResultSet resultSet = pstmt.executeQuery();

            if (!resultSet.next()) {
                log.warn("There is no cat with id: " + catId);
                 throw new CustomerException("no cat found");

            }

            catId = resultSet.getInt("catId");
            String catname = resultSet.getString("catname");
            String catcolor = resultSet.getString("catcolor");
            Health health;
            if (resultSet.getString("health") == null) {
                health = null;

            } else {

                health = Health.valueOf(resultSet.getString("health"));
            }
            Sex sex;
            if (resultSet.getString("sex") == null) {
                sex = null;

            } else {
                sex = Sex.valueOf(resultSet.getString("sex"));
            }
            int ownerid = resultSet.getInt("userid");
            Cat cat = new Cat(catId, catname, catcolor, health, sex, ownerid);

            return cat;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    public List<Cat> getCatsNotMatchedYet(int catId, String raceCats, Health health, Sex sex,int ownerid ) {
        log.info("getCats: raceCats " + raceCats + " health " + health + "sex" + sex);


        List<Cat> catsList = new ArrayList<>();
        String queryGetCats = """
                SELECT *
                FROM cat c1 INNER JOIN cat c2 ON (c1.catid < c2.catid) AND (c1.catid = ? OR c2.catid = ?)
                    LEFT JOIN relationshipcats AS r
                    ON (c1.catid = r.firstcatid AND c2.catid = r.secondcatid) OR
                    (c1.catid = r.secondcatid AND c2.catid = r.firstcatid)
                WHERE
                      r.firstcatid is NULL
                      AND c1.catcolor = ? AND c2.catcolor = ?
                      AND c1.health = ? AND c2.health = ?
                      AND c1.userid <> c2.userid
                      AND c1.sex <> c2.sex
                """;

        try (PreparedStatement pstmt = con.prepareStatement(queryGetCats);) {

            pstmt.setInt(1, catId);
            pstmt.setInt(2, catId);
            pstmt.setString(3, raceCats);
            pstmt.setString(4, raceCats);
            pstmt.setString(5, String.valueOf(health));
            pstmt.setString(6, String.valueOf(health));

            ResultSet resultSet = pstmt.executeQuery();
            while (resultSet.next()) {
                var findToPair =resultSet.getInt("");
                String catname = resultSet.getString("catname");
                String catcolor = resultSet.getString("catcolor");
                ownerid = resultSet.getInt("userid");
                Health healthFromDb;
                if (resultSet.getString("health") == null) {
                    healthFromDb = null;

                } else {

                    healthFromDb = Health.valueOf(resultSet.getString("health"));
                }
                Sex sexFromDb;
                if (resultSet.getString("sex") == null) {
                    sexFromDb = null;

                } else {

                    sexFromDb = Sex.valueOf(resultSet.getString("sex"));
                }
                Cat cat = new Cat(findToPair,catname, catcolor, healthFromDb, sexFromDb, ownerid);
                catsList.add(cat);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return catsList;

    }


    }

