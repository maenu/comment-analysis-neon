package ch.unibe.scg.comment.analysis.neon.cli.task;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
				Statement statement = connection.createStatement()
		) {
			statement.executeUpdate("PRAGMA foreign_keys = on");
			this.checkPrecondition(statement);
			List<String> categories = this.categories(statement);
			try (
					ResultSet result = statement.executeQuery("SELECT * FROM " + this.data + "_raw");
					PreparedStatement insert = this.insert(connection, categories)
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
		return s.toLowerCase().replaceAll("\r\n|\r", "\n") // reduce line endings -> line endings are now \n
				.replaceAll("[^a-z0-9,.!? \n]", " ") // reduce alphabet -> string is now [a-z0-9,.!? \n]+
				.replaceAll("([0-9]+)\\.([0-9]+)", "$1$2") // replace floats
				.replaceAll(" +", " ") // reduce spaces -> spaces are now single spaces
				.replaceAll("^[ \n]+", "") // reduce document start -> document does not start with whitespace
				.replaceAll("[ \n]+$", "") // reduce document end -> document does not end with whitespace
				.replaceAll("\n ", "\n") // reduce line starts -> lines do not start with spaces
				.replaceAll(" \n", "\n") // reduce line ends -> lines do not end with spaces
				.replaceAll("\n(\n+)", "\n\n") // reduce newlines -> line ends are now \n or \n\n
				.replaceAll("([^.!?\n])\n([^\n])", "$1 $2") // join non-sentence lines -> whole sentence on same line
				.replaceAll(" ?([.!?])[ .!?]*",
						"$1"
				) // normalize sentence ends -> multi-terminated is now single-terminated
				.replaceAll(" ?,[ ,]*", ", ") // normalize sentence ends -> multi-terminated is now single-terminated
				.replaceAll("([,.!?])([^ ])",
						"$1 $2"
				) // split joined sentences -> sentences and parts are separated by space
				.replaceAll("(\n|^)[ .!?]+", "$1") // ensure line starts with non-separators
				.replaceAll("(^|[^a-z0-9])e\\.? ?g\\.? ?($|[^.a-z0-9])", "$1eg$2") // replace eg
				.replaceAll("(^|[^a-z0-9])i\\.? ?e\\.? ?($|[^.a-z0-9])", "$1ie$2") // replace ie
				.replaceAll("(^|[^a-z0-9])etc\\.? ?($|[^.a-z0-9])", "$1etc$2") // replace etc
				.replaceAll("[^.!?\n]{400}[^ \n]*[ \n]?",
						"$0\n\n"
				); // split long sentences -> sentences are now at most 400 characters plus one word long
	}

	private PreparedStatement insert(Connection connection, List<String> categories) throws SQLException {
		return connection.prepareStatement(
				"INSERT INTO " + this.data + "_preprocessed (class, stratum, comment, " + String.join(",",
						categories.stream().map(c -> String.format("'%s'", c)).collect(Collectors.toList())
				) + ") VALUES (?, ?, ?, " + String.join(",",
						categories.stream().map(c -> "?").collect(Collectors.toList())
				) + ")");
	}

	private List<String> categories(Statement statement) throws SQLException {
		List<String> categories = new ArrayList<>();
		try (
				ResultSet result = statement.executeQuery(
						"SELECT name FROM PRAGMA_TABLE_INFO('" + this.data + "_preprocessed')")
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
