package ch.unibe.scg.comment.analysis.neon.cli.task;

import org.neon.model.Condition;
import org.neon.model.Heuristic;
import org.neon.pathsFinder.engine.Parser;
import org.neon.pathsFinder.engine.PathsFinder;
import org.neon.pathsFinder.engine.XMLWriter;
import org.neon.pathsFinder.model.GrammaticalPath;
import org.neon.pathsFinder.model.Sentence;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class ExtractHeuristics {

    private final String database;
    private final String data;

    public ExtractHeuristics(String database, String data) {
        this.database = database;
        this.data = data;
    }

    public void run() throws Exception {
        try (
                Connection connection = DriverManager.getConnection("jdbc:sqlite:" + this.database);
                Statement statement = connection.createStatement();
                PreparedStatement insert = connection.prepareStatement("INSERT INTO " + this.data + "_heuristics (partition, category, heuristics) VALUES (?, ?, ?)");
        ) {
            statement.executeUpdate("PRAGMA foreign_keys = on");
            this.checkPrecondition(statement);
            for (String category : this.categories(statement)) {
                Map<Integer, List<String>> partitions = new HashMap<>();
                try (ResultSet result = statement.executeQuery("SELECT partition, \"" + category + "\" FROM " + this.data + "_preprocessed JOIN pharo_partition on (pharo_partition.class = " + this.data + "_preprocessed.class) WHERE '" + category + "' IS NOT NULL")) {
                    while (result.next()) {
                        int partition = result.getInt("partition");
                        if (!partitions.containsKey(partition)) {
                            partitions.put(partition, new ArrayList<>());
                        }
                        partitions.get(partition).add(result.getString(category));
                    }
                }
                for (Map.Entry<Integer, List<String>> partition : partitions.entrySet()) {
                    insert.setInt(1, partition.getKey());
                    insert.setString(2, category);
                    insert.setString(3, this.heuristics(category, partition.getValue()));
                    insert.executeUpdate();
                }
            }
        }
    }

    public String heuristics(String category, List<String> entries) throws Exception {
        ArrayList<Sentence> sentences = Parser.getInstance().parse(String.join("\n\n", entries));
        ArrayList<GrammaticalPath> paths = PathsFinder.getInstance().discoverCommonPaths(sentences);
        ArrayList<Heuristic> heuristics = paths.stream().map(p -> {
            Heuristic heuristic = new Heuristic();
            heuristic.setConditions(p.getConditions().stream().map(s -> {
                Condition condition = new Condition();
                condition.setConditionString(s);
                return condition;
            }).collect(Collectors.toCollection(ArrayList::new)));
            heuristic.setType(p.getDependenciesPath());
            heuristic.setSentence_type(p.identifySentenceType());
            heuristic.setText(p.getTemplateText());
            heuristic.setSentence_class(category);
            return heuristic;
        }).collect(Collectors.toCollection(ArrayList::new));
        Path path = Files.createTempFile("heuristics", ".xml");
        XMLWriter.addXMLHeuristics(path.toFile(), heuristics);
        String result = Files.readString(path);
        path.toFile().delete();
        return result;
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
        try (ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM " + this.data + "_heuristics")) {
            result.next();
            if (result.getInt(1) > 0) {
                throw new IllegalStateException(String.format("%s_heuristics is not empty", this.data));
            }
        }
    }

}
