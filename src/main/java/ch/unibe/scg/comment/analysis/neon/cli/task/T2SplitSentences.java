package ch.unibe.scg.comment.analysis.neon.cli.task;

import edu.stanford.nlp.simple.Sentence;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Split the sentences using Neon parser to keep the consistent with heuristics.
 *
 */
public class T2SplitSentences {

	private final String database;
	private final String data;

	public T2SplitSentences(String database, String data) {
		this.database = database;
		this.data = data;
	}

	public void run() throws SQLException, IOException {
		try (
				Connection connection = DriverManager.getConnection("jdbc:sqlite:" + this.database);
				Statement statement = connection.createStatement()
		) {
			statement.executeUpdate("PRAGMA foreign_keys = on");
			statement.executeUpdate(Utility.resource("sql/2_sentence.sql").replaceAll("\\{\\{data}}", this.data));
			List<String> categories = this.categories(statement);
			try (
					PreparedStatement insert = connection.prepareStatement(
							"INSERT INTO " + this.data + "_2_sentence (class, category, sentence) VALUES (?, ?, ?)");
					ResultSet result = statement.executeQuery("SELECT * FROM " + this.data + "_1_preprocessed")
			) {
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
		ArrayList<String> processedSentences = new ArrayList<String>();
		String[] sentences = text.split("(\\n|:)");
		processedSentences.addAll(Arrays.asList(sentences));
		return processedSentences;
	}

	private List<String> categories(Statement statement) throws SQLException {
		List<String> categories = new ArrayList<>();
		try (
				ResultSet result = statement.executeQuery(
						"SELECT name FROM PRAGMA_TABLE_INFO('" + this.data + "_0_raw')")
		) {
			while (result.next()) {
				categories.add(result.getString("name"));
			}
		}
		categories.remove("class");
		categories.remove("stratum");
		categories.remove("comment");
		return categories;
	}

}
