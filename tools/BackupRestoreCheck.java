import java.sql.*;
import java.util.List;

/** Read-only checks against an isolated restored development database. */
public class BackupRestoreCheck {
    public static void main(String[] args) throws Exception {
        if(args.length!=1 || !args[0].matches("jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/matchcats_dev"))
            throw new IllegalArgumentException("Expected an isolated localhost development database URL");
        try(var connection=DriverManager.getConnection(args[0],"postgres","")) {
            connection.setReadOnly(true);
            for(String table:List.of("mc_accounts","mc_breeders","mc_cats","mc_cat_photos","mc_conversations","mc_messages","mc_documents","mc_cat_pairs","mc_message_reads")) {
                try(var query=connection.createStatement();var rows=query.executeQuery("SELECT count(*) FROM "+table)) {
                    rows.next();System.out.println(table+"="+rows.getLong(1));
                }
            }
            try(var query=connection.createStatement();var rows=query.executeQuery("SELECT count(*) FROM flyway_schema_history WHERE NOT success")) {
                rows.next();if(rows.getInt(1)!=0)throw new IllegalStateException("Failed schema migration in backup");
            }
            try(var query=connection.createStatement();var rows=query.executeQuery("SELECT count(*) FROM pg_constraint WHERE connamespace='public'::regnamespace AND NOT convalidated")) {
                rows.next();if(rows.getInt(1)!=0)throw new IllegalStateException("Unvalidated database constraint");
            }
            try(var query=connection.createStatement();var rows=query.executeQuery("SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank DESC LIMIT 1")) {
                if(!rows.next())throw new IllegalStateException("Missing schema migrations");
                System.out.println("schemaVersion="+rows.getString(1));
            }
            System.out.println("PASS: restored tables are readable and schema checks passed");
        }
    }
}
