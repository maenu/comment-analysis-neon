package ch.unibe.scg.comment.analysis.neon.cli.task;

import org.neon.engine.Parser;
import org.neon.engine.XMLWriter;
import org.neon.model.Result;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ClassifyComments {

	private final String database;
	private final String data;
	private final int buckets;

	public ClassifyComments(String database, String data, int buckets) {
		this.database = database;
		this.data = data;
		this.buckets = buckets;
	}

	public static void classify(String[] args) throws Exception {
		// Initialize the text that needs to be classified
		String text = Files.readString(Paths.get(args[0]));
		// The xml file containing the heuristics to use
		File heuristics = new File(args[1]);
		// The xml file that will contain the output of the classification
		File output = new File(args[2]);
		// The parser returns a collection of results
		ArrayList<Result> results = Parser.getInstance().extract(text, heuristics);
		// Export the results in the destination file
		XMLWriter.addXMLSentences(output, results);
	}

	public void run() throws SQLException {
		try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + this.database)) {
			Statement statement = connection.createStatement();
			// enable foreign keys
			statement.executeUpdate("PRAGMA foreign_keys = on");
			this.checkPrecondition(statement);
			List<List<String>> buckets = this.buckets(statement);
			// create tables
			for (int i = 0; i < buckets.size(); i = i + 1) {
				statement.executeUpdate(
						"CREATE TABLE " + this.data + "_bucket_" + i + " (class TEXT NOT NULL REFERENCES " + this.data
								+ "(class), PRIMARY KEY(class))");
				statement.executeUpdate("INSERT INTO " + this.data + "_bucket_" + i + " (class) VALUES " + String.join(
						",",
						buckets.get(i).stream().map(c -> "('" + c + "')").collect(Collectors.toList())
				));
			}
		}
	}

	private void checkPrecondition(Statement statement) throws SQLException {
		try (
				ResultSet result = statement.executeQuery(
						"SELECT name FROM sqlite_master WHERE type ='table' AND name LIKE '" + this.data + "_bucket_%'")
		) {
			if (result.next()) {
				throw new IllegalStateException("table like " + this.data
						+ "_bucket_%, will not perform tasks until no table like this exists to avoid overwriting data");
			}
		}
	}

	private List<List<String>> buckets(Statement statement) throws SQLException {
		try (ResultSet result = statement.executeQuery("SELECT class, stratum FROM " + this.data)) {
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
			// build buckets from strata
			List<List<String>> buckets = new ArrayList<>();
			for (int i = 0; i < this.buckets; i = i + 1) {
				buckets.add(new ArrayList<>());
			}
			// round-robin, fill buckets from strata, one at a time
			for (List<String> s : strata.values()) {
				while (!s.isEmpty()) {
					for (int i = 0; i < this.buckets; i = i + 1) {
						Optional<String> clazz = this.random(s);
						if (clazz.isPresent()) {
							s.remove(clazz.get());
							buckets.get(i).add(clazz.get());
						}
					}
				}
			}
			return buckets;
		}
	}

	private <E> Optional<E> random(Collection<E> e) {
		return e.stream().skip((int) (e.size() * Math.random())).findFirst();
	}

}
