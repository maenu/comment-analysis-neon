package ch.unibe.scg.comment.analysis.neon.cli.task;

import org.neon.pathsFinder.engine.Parser;
import org.neon.pathsFinder.model.Sentence;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class SplitSentences {

    private final String database;
    private final String data;

    public SplitSentences(String database, String data) {
        this.database = database;
        this.data = data;
    }

    public void run() throws SQLException {
        try (
                Connection connection = DriverManager.getConnection("jdbc:sqlite:" + this.database);
                Statement statement = connection.createStatement();
                PreparedStatement insert = connection.prepareStatement("INSERT INTO " + this.data + "_sentence (class, category, sentence) VALUES (?, ?, ?)");
        ) {
            statement.executeUpdate("PRAGMA foreign_keys = on");
            this.checkPrecondition(statement);
            List<String> categories = this.categories(statement);
            try (ResultSet result = statement.executeQuery("SELECT * FROM " + this.data + "_preprocessed")) {
                while (result.next()) {
                    String clazz = result.getString("class");
                    this.sentences(insert, clazz, "comment", result.getString("comment"));
                    for (String category : categories) {
                        this.sentences(insert, clazz, category, result.getString(category));
                    }
                }
            }
        }
    }

    private void sentences(PreparedStatement insert, String clazz, String category, String text) throws SQLException {
        if (text == null) {
            return;
        }
        for (String sentence : this.split(text)) {
            insert.setString(1, clazz);
            insert.setString(2, category);
            insert.setString(3, sentence);
            insert.executeUpdate();
        }
    }

    private List<String> split(String text) {
        return Parser.getInstance().parse(text).stream().map(Sentence::getText).collect(Collectors.toList());
    }

    private List<String> categories(Statement statement) throws SQLException {
        List<String> categories = new ArrayList<>();
        try (ResultSet result = statement.executeQuery("SELECT name FROM PRAGMA_TABLE_INFO('" + this.data + "_raw')")) {
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
        try (ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM " + this.data + "_sentence")) {
            result.next();
            if (result.getInt(1) > 0) {
                throw new IllegalStateException(String.format("%s_sentence is not empty", this.data));
            }
        }
    }

}
