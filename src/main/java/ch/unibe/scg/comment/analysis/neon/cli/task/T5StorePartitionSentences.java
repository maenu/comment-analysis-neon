package ch.unibe.scg.comment.analysis.neon.cli.task;

import org.neon.engine.XMLReader;
import org.neon.model.Condition;
import org.neon.model.Heuristic;
import org.neon.pathsFinder.engine.Parser;
import org.neon.pathsFinder.engine.PathsFinder;
import org.neon.pathsFinder.engine.XMLWriter;
import org.neon.pathsFinder.model.GrammaticalPath;
import org.neon.pathsFinder.model.Sentence;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.core.stemmers.IteratedLovinsStemmer;
import weka.core.stopwords.Rainbow;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.StringToWordVector;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Store the sentences for each training and testing partition
 * @datbase input database (sqlite for now)
 * @data language under analysis
 */
public class T5StorePartitionSentences {

	private final String database;
	private final String data;

	public T5StorePartitionSentences(String database, String data) {
		this.database = database;
		this.data = data;
	}

	public void run() throws Exception {
		try (
				Connection connection = DriverManager.getConnection("jdbc:sqlite:" + this.database);
				Statement statement = connection.createStatement()
		) {
			statement.executeUpdate("PRAGMA foreign_keys = on");
			statement.executeUpdate(Utility.resource("sql/5_sentences_partitions.sql").replaceAll("\\{\\{data}}", this.data));
			Map<Integer, Map<String, List<String>>> partitions = new HashMap<>();
			for (String category : this.categories(statement)) {
				try (
						ResultSet result = statement.executeQuery("SELECT partition, comment_sentence FROM " + this.data
								+ "_3_sentence_mapping_clean JOIN " + this.data + "_4_sentence_partition on ("
								+ this.data + "_4_sentence_partition.comment_sentence_id = " + this.data
								+ "_3_sentence_mapping_clean.comment_sentence_id) WHERE category = \"" + category
								+ "\"")
				) {
					while (result.next()) {
						int partition = result.getInt("partition");
						if (!partitions.containsKey(partition)) {
							partitions.put(partition, new HashMap<>());
						}
						if (!partitions.get(partition).containsKey(category)) {
							partitions.get(partition).put(category, new ArrayList<>());
						}
						partitions.get(partition).get(category).add(result.getString("comment_sentence"));
					}
				}
			}
			try (
					PreparedStatement insert = connection.prepareStatement("INSERT INTO " + this.data
							+ "_5_sentences_partitions (comment_sentence, partition, category) VALUES (?, ?, ?)")
			) {
				for (Map.Entry<Integer, Map<String, List<String>>> partition : partitions.entrySet()) {
					for (Map.Entry<String, List<String>> category :partition.getValue().entrySet()){
						for (String sentence: category.getValue()) {
							insert.setString(1, sentence);
							insert.setInt(2, partition.getKey());
							insert.setString(3, category.getKey());
							insert.executeUpdate();
						}
					}
				}
			}
		}
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
