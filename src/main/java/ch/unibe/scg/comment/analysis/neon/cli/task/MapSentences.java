package ch.unibe.scg.comment.analysis.neon.cli.task;

import java.sql.*;
import java.util.*;

public class MapSentences {

    private final String database;
    private final String data;

    public MapSentences(String database, String data) {
        this.database = database;
        this.data = data;
    }

    public void run() throws SQLException {
        try (
                Connection connection = DriverManager.getConnection("jdbc:sqlite:" + this.database);
                Statement statement = connection.createStatement();
                PreparedStatement insert = connection.prepareStatement("INSERT INTO " + this.data + "_mapping (comment_sentence_id, category_sentence_id, strategy, similarity) VALUES (?, ?, ?, ?)");
        ) {
            statement.executeUpdate("PRAGMA foreign_keys = on");
            this.checkPrecondition(statement);
            try (ResultSet result = statement.executeQuery("SELECT class FROM " + this.data + "_preprocessed")) {
                while (result.next()) {
                    String clazz = result.getString("class");
                    Map<String, Map<Integer, String>> sentences = this.sentences(connection, clazz);
                    for (Map.Entry<Integer, String> commentEntry : sentences.get("comment").entrySet()) {
                        int commentSentenceId = commentEntry.getKey();
                        String commentSentence = commentEntry.getValue();
                        for (Map.Entry<String, Map<Integer, String>> categoryEntry : sentences.entrySet()) {
                            if (categoryEntry.getKey().equals("comment")) {
                                continue;
                            }
                            for (Map.Entry<Integer, String> categorySentenceEntry : categoryEntry.getValue().entrySet()) {
                                int categorySentenceId = categorySentenceEntry.getKey();
                                String categorySentence = categorySentenceEntry.getValue();
                                if (commentSentence.equals(categorySentence)) {
                                    this.mapping(insert, commentSentenceId, categorySentenceId, "equals", 1.0);
                                } else if (commentSentence.contains(categorySentence)) {
                                    this.mapping(insert, commentSentenceId, categorySentenceId, "contains", 1.0 * categorySentence.length() / commentSentence.length());
                                } else if (commentSentence.replaceAll("[.!?]$", "").contains(categorySentence.replaceAll("[.!?]$", ""))) {
                                    this.mapping(insert, commentSentenceId, categorySentenceId, "contains-stripped", 1.0 * categorySentence.length() / commentSentence.length());
                                } else if (commentSentence.replaceAll("[^a-z0-9]", "").contains(categorySentence.replaceAll("[^a-z0-9]", ""))) {
                                    this.mapping(insert, commentSentenceId, categorySentenceId, "contains-a-z-0-9", 1.0 * categorySentence.length() / commentSentence.length());
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void mapping(PreparedStatement insert, int commentSentenceId, int categorySentenceId, String strategy, double similarity) throws SQLException {
        insert.setInt(1, commentSentenceId);
        insert.setInt(2, categorySentenceId);
        insert.setString(3, strategy);
        insert.setDouble(4, similarity);
        insert.executeUpdate();
    }

    public Map<String, Map<Integer, String>> sentences(Connection connection, String clazz) throws SQLException {
        Map<String, Map<Integer, String>> sentences = new HashMap<>();
        try (
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("SELECT id, category, sentence FROM " + this.data + "_sentence WHERE class = '" + clazz + "'");
        ) {
            while (result.next()) {
                int id = result.getInt("id");
                String category = result.getString("category");
                String sentence = result.getString("sentence");
                if (!sentences.containsKey(category)) {
                    sentences.put(category, new HashMap<>());
                }
                sentences.get(category).put(id, sentence);
            }
        }
        return sentences;
    }

    private void checkPrecondition(Statement statement) throws SQLException {
        try (ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM " + this.data + "_mapping")) {
            result.next();
            if (result.getInt(1) > 0) {
                throw new IllegalStateException(String.format("%s_label is not empty", this.data));
            }
        }
    }

}