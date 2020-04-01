package ch.unibe.scg.comment.analysis.neon.cli.task;

import org.neon.model.Condition;
import org.neon.model.Heuristic;
import org.neon.pathsFinder.engine.Parser;
import org.neon.pathsFinder.engine.PathsFinder;
import org.neon.pathsFinder.engine.XMLWriter;
import org.neon.pathsFinder.model.GrammaticalPath;
import org.neon.pathsFinder.model.Sentence;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class ExtractHeuristics {

    private final String database;
    private final String data;
    private final String sentenceClass;

    public ExtractHeuristics(String database, String data, String sentenceClass) {
        this.database = database;
        this.data = data;
        this.sentenceClass = sentenceClass;
    }

    public void run() throws SQLException {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + this.database)) {
            Statement statement = connection.createStatement();
            // enable foreign keys
            statement.executeUpdate("PRAGMA foreign_keys = on");
        }
    }

    public String heuristics(String text) throws Exception {
        ArrayList<Sentence> sentences = Parser.getInstance().parse(text);
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
            heuristic.setSentence_class(this.sentenceClass);
            return heuristic;
        }).collect(Collectors.toCollection(ArrayList::new));
        Path path = Files.createTempFile("heuristics", ".xml");
        XMLWriter.addXMLHeuristics(path.toFile(), heuristics);
        String result = Files.readString(path);
        path.toFile().delete();
        return result;
    }

}
