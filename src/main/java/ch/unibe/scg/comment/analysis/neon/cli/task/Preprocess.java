package ch.unibe.scg.comment.analysis.neon.cli.task;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Preprocess {

    private final String database;
    private final String data;

    public Preprocess(String database, String data) {
        this.database = database;
        this.data = data;
    }

    public void run() throws SQLException {
        try (
                Connection connection = DriverManager.getConnection("jdbc:sqlite:" + this.database);
                Statement statement = connection.createStatement();
        ) {
            statement.executeUpdate("PRAGMA foreign_keys = on");
            this.checkPrecondition(statement);
            List<String> categories = this.categories(statement);
            try (
                    ResultSet result = statement.executeQuery("SELECT * FROM " + this.data + "_raw");
                    PreparedStatement insert = this.insert(connection, categories);
            ) {
                while (result.next()) {
                    String clazz = result.getString("class");
                    int stratum = result.getInt("stratum");
                    String comment = result.getString("comment");
                    insert.setString(1, clazz);
                    insert.setInt(2, stratum);
                    insert.setString(3, this.preprocess(comment));
                    for (int i = 0; i < categories.size(); i = i + 1) {
                        insert.setString(4 + i, this.preprocess(result.getString(4 + i)));
                    }
                    insert.executeUpdate();
                }
            }
        }
    }

    private String preprocess(String s) {
        if (s == null) {
            return null;
        }
        return s.replaceAll("\r\n|\n", "\n")
                .replaceAll("[\t ]+", " ")
                .replaceAll("[^A-Za-z0-9,.!? \n]", " ")
                .replaceAll("\n( +)", "\n")
                .replaceAll("( +)\n", "\n")
                .replaceAll("([^.!?\n])\n([^\n])", "$1 $2")
                .replaceAll(" ( +)", " ")
                .replaceAll("\n\n\n", "\n\n")
                .replaceAll("([,.!?])+", "$1")
                .replaceAll(" ([,.!?])", "$1")
                .replaceAll("[^\n.!?]{400}([^ ]*)", "$0.\n");
    }

    private PreparedStatement insert(Connection connection, List<String> categories) throws SQLException {
        return connection.prepareStatement("INSERT INTO " + this.data + "_preprocessed (class, stratum, comment, " + String.join(",", categories.stream().map(c -> String.format("'%s'", c)).collect(Collectors.toList())) + ") VALUES (?, ?, ?, " + String.join(",", categories.stream().map(c -> "?").collect(Collectors.toList())) + ")");
    }

    private List<String> categories(Statement statement) throws SQLException {
        List<String> categories = new ArrayList<>();
        try (ResultSet result = statement.executeQuery("SELECT name FROM PRAGMA_TABLE_INFO('" + this.data + "_preprocessed')")) {
            while (result.next()) {
                categories.add(result.getString("name"));
            }
        }
        categories.remove("class");
        categories.remove("stratum");
        categories.remove("comment");
        return categories;
    }

    private void checkPrecondition(Statement statement) throws SQLException {
        try (ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM " + this.data + "_raw")) {
            result.next();
            if (result.getInt(1) == 0) {
                throw new IllegalStateException(String.format("%s_raw is empty", this.data));
            }
        }
        try (ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM " + this.data + "_preprocessed")) {
            result.next();
            if (result.getInt(1) > 0) {
                throw new IllegalStateException(String.format("%s_preprocessed is not empty", this.data));
            }
        }
    }

}
