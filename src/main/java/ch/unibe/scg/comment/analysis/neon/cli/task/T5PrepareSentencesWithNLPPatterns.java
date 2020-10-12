package ch.unibe.scg.comment.analysis.neon.cli.task;

import org.neon.model.Result;
import org.neon.engine.Parser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class T5PrepareSentencesWithNLPPatterns {

	private final String database;
	private final String data;
	private final int extractorsPartition;
	private File heuristicFile;

	public T5PrepareSentencesWithNLPPatterns(String database, String data, int extractorsPartition) {
		this.database = database;
		this.data = data;
		this.extractorsPartition = extractorsPartition;
	}

	public void run() throws SQLException, IOException {
		try (
				Connection connection = DriverManager.getConnection("jdbc:sqlite:" + this.database);
				Statement statement = connection.createStatement()
		) {
			statement.executeUpdate("PRAGMA foreign_keys = on");
			statement.executeUpdate(Utility.resource("sql/5_sentence_heuristic_mapping.sql")
					.replaceAll("\\{\\{data}}", this.data));
			PreparedStatement insert = connection.prepareStatement("INSERT INTO " + this.data + "_5_sentence_heuristic_mapping (comment_sentence_id, comment_sentence, heuristics, category) VALUES (?, ?, ?, ?)");

			//Get the neon generated heuristics and store them in a file
			try (
					ResultSet result = statement.executeQuery(
							"SELECT heuristics FROM " + this.data + "_5_extractors WHERE partition = "
									+ this.extractorsPartition + "")
			) {
				Path heuristicsPath = Files.createTempFile("heuristics", ".xml");
				Files.write(heuristicsPath, result.getBytes("heuristics"));
				this.heuristicFile = heuristicsPath.toFile();
			}

			for (String category : this.categories(statement)) {
				try (
						ResultSet result = statement.executeQuery(
								"SELECT comment_sentence_id, comment_sentence, category  FROM " + this.data + "_3_sentence_mapping_clean WHERE category = \"" + category
										+ "\"")
				) {
					while (result.next()) {
						int id = result.getInt("comment_sentence_id");
						String sentence = result.getString("comment_sentence");
						String sentence_category = result.getString("category");

						String heuristics = this.predictCategory(sentence);
						insert.setInt(1,id);
						insert.setString(2,sentence);
						insert.setString(3,heuristics);
						insert.setString(4,sentence_category);
						insert.execute();
					}
				}
			}
		}
	}

	/**
	 *
	 * @param text the sentence to classify
	 * @return
	 * @throws SQLException
	 * @throws IOException
	 */
	private String predictCategory(String text) throws IOException {
		ArrayList<Result> results = new ArrayList<>();
		ArrayList<String> sentence_heuristics =  new ArrayList<>();

			  results = Parser.getInstance().extract(text, this.heuristicFile);
			  for(Result r: results){
				  System.out.println("SentenceClass: " + r.getSentenceClass());
				  System.out.println("Heuristic: " +  r.getHeuristic());
				  System.out.println("heuristic_category: "+ this.featureName(r.getSentenceClass(),r.getHeuristic()));
				  sentence_heuristics.add(r.getHeuristic());
			  }
		return sentence_heuristics.stream().collect(Collectors.joining("#"));
	}

	/**
	 * @return "categories"
	 */
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

	/**
	 * @return "heuristic-[category]-[heuristic]"
	 */
	private String featureName(String category, String heuristic) {
		return String.format("heuristic-%s-%s", category, heuristic);
	}
}
