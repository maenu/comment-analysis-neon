package ch.unibe.scg.comment.analysis.neon.cli.task;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class Partition {

    private final String database;
    private final String data;
    private final int partitions;

    public Partition(String database, String data, int partitions) {
        this.database = database;
        this.data = data;
        this.partitions = partitions;
    }

    public void run() throws SQLException {
        try (
                Connection connection = DriverManager.getConnection("jdbc:sqlite:" + this.database);
                Statement statement = connection.createStatement();
                PreparedStatement insert = connection.prepareStatement("INSERT INTO " + this.data + "_partition (class, partition) VALUES (?, ?)");
        ) {
            // enable foreign keys
            statement.executeUpdate("PRAGMA foreign_keys = on");
            this.checkPrecondition(statement);
            try (ResultSet result = statement.executeQuery("SELECT class, stratum FROM " + this.data + "_preprocessed")) {
                // build strata from raw data
                Map<Integer, List<String>> strata = new HashMap<>();
                while (result.next()) {
                    String clazz = result.getString("class");
                    int i = result.getInt("stratum");
                    if (!strata.containsKey(i)) {
                        strata.put(i, new ArrayList<>());
                    }
                    strata.get(i).add(clazz);
                }
                // round-robin, fill partitions from strata, one at a time
                for (List<String> s : strata.values()) {
                    while (!s.isEmpty()) {
                        for (int i = 0; i < this.partitions; i = i + 1) {
                            Optional<String> clazz = this.random(s);
                            if (clazz.isPresent()) {
                                s.remove(clazz.get());
                                insert.setString(1, clazz.get());
                                insert.setInt(2, i);
                                insert.executeUpdate();
                            }
                        }
                    }
                }
            }
        }
    }

    private <E> Optional<E> random(Collection<E> e) {
        return e.stream().skip((int) (e.size() * Math.random())).findFirst();
    }

    private void checkPrecondition(Statement statement) throws SQLException {
        try (ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM " + this.data + "_partition")) {
            result.next();
            if (result.getInt(1) > 0) {
                throw new IllegalStateException(String.format("%s_partition is not empty", this.data));
            }
        }
    }

}
