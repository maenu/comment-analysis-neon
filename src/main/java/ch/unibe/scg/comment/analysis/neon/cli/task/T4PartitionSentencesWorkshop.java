package ch.unibe.scg.comment.analysis.neon.cli.task;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Prepare the partitions based on a different logic than T4PartitionSentences
 * @partitions number of partitions to create. Currently we have training (0_0) and testing (1_0) split.
 */
public class T4PartitionSentencesWorkshop {

	private final String database;
	private final String data;
	private final int partition_num;

	public T4PartitionSentencesWorkshop(String database, String data, int part) {
		this.database = database;
		this.data = data;
		this.partition_num = part;
	}

	public void run() throws SQLException, IOException {
		try (
				Connection connection = DriverManager.getConnection("jdbc:sqlite:" + this.database);
				Statement statement = connection.createStatement()
		) {
			statement.executeUpdate("PRAGMA foreign_keys = on");
			statement.executeUpdate(Utility.resource("sql/4_sentence_partition_workshop.sql")
					.replaceAll("\\{\\{data}}", this.data));
			try (
					PreparedStatement insert = connection.prepareStatement("INSERT INTO " + this.data
							+ "_4_sentence_partition_workshop (partition, category, comment_sentence, class) VALUES (?, ?, ?, ?)");
					ResultSet result = statement.executeQuery(
							"SELECT comment_sentence, comment_sentence_id, stratum, class, category FROM " + this.data
									+ "_3_sentence_mapping_clean")
			) {
				Map<Integer, Map<String, Map<String,String> >> partitions = new HashMap<>(); //Strata contains <partition_number, <comment_sentence_id, <comment_sentence, class_name, category>>>
				int partitionsSize =  this.partition_num; //number of partition to make
				ArrayList<Integer> processed = new ArrayList();
				while (result.next()) {
					int partition_num = 0;
					int id = result.getInt("comment_sentence_id");
					String comment_sentence = result.getString("comment_sentence");
					String category = result.getString("category");
					String class_name = result.getString("class");

					//keep track of ids that are processed. Comment_sentence_id are unique
					if(!processed.contains(id)) {
						processed.add(id);
						if((id + 1) % partitionsSize == 0) { //assign every partition_numth to test partition, remaining to training
							partition_num = 1; //for testing data
							if(!partitions.containsKey(partition_num)) {
								partitions.put(partition_num, new HashMap<>());
							}
							if(!partitions.get(partition_num).containsKey(category)) {
								partitions.get(partition_num).put(category, new HashMap<>());
							}
							if(!partitions.get(partition_num).get(category).containsKey(comment_sentence)){
								partitions.get(partition_num).get(category).put(comment_sentence,class_name);
							}
						}
						else{
							partition_num = 0; //for training data
							if(!partitions.containsKey(partition_num)) {
								partitions.put(partition_num, new HashMap<>());
							}
							if(!partitions.get(partition_num).containsKey(category)) {
								partitions.get(partition_num).put(category, new HashMap<>());
							}
							if(!partitions.get(partition_num).get(category).containsKey(comment_sentence)){
								partitions.get(partition_num).get(category).put(comment_sentence,class_name);
							}
						}
					}
				}

				for (Map.Entry<Integer, Map<String, Map<String, String>>> partition : partitions.entrySet()) {
					for (Map.Entry<String, Map<String, String>> category_temp :partition.getValue().entrySet()){
						for (Map.Entry<String, String> a_comment: category_temp.getValue().entrySet()) {
							insert.setInt(1, partition.getKey());
							insert.setString(2, category_temp.getKey());
							insert.setString(3, a_comment.getKey());
							insert.setString(4, a_comment.getValue());
							insert.executeUpdate();
						}
					}
				}

			}
		}
	}
}
