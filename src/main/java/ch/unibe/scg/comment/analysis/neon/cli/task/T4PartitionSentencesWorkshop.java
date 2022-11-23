package ch.unibe.scg.comment.analysis.neon.cli.task;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Prepare the partitions based on a different logic than T4PartitionSentences
 * @partitions number of partitions to create. Currently, we have training (0_0) and testing (1_0) split.
 */
public class T4PartitionSentencesWorkshop {
	private final String database;
	private final String data;
	private final int[] partitions;

	public T4PartitionSentencesWorkshop(String database, String data, int[] partitions) {
		this.database = database;
		this.data = data;
		this.partitions = partitions;
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
							+ "_4_sentence_partition_workshop (comment_sentence_id, partition, category) VALUES (?, ?, ?)");
					ResultSet result = statement.executeQuery(
							"SELECT comment_sentence_id, stratum, category FROM " + this.data
									+ "_3_sentence_mapping_clean")
			) {
				// build category distributions as per strata from raw data
				Map<String, Map<Integer, Set<Integer>>> partitions_category = new HashMap<>();
				while (result.next()) {
					int id = result.getInt("comment_sentence_id");
					int stratum = result.getInt("stratum");
					String category = result.getString("category");
					if (!partitions_category.containsKey(category)) {
						partitions_category.put(category, new HashMap<>());
					}
					if (!partitions_category.get(category).containsKey(stratum)) {
						partitions_category.get(category).put(stratum, new HashSet<>());
					}
					partitions_category.get(category).get(stratum).add(id);
				}

				// round-robin, fill partitions from strata, one at a time
				// select from the smallest stratum for each category to achieve best balancing
				for(String aCategory:  partitions_category.keySet())
				for (Map.Entry<Integer, Set<Integer>> strata : partitions_category.get(aCategory).entrySet()) {
					// treat strata as independent
					while(!strata.getValue().isEmpty()) {
							//assign to partitions based on proportion of the training and testing
							int total_instances = strata.getValue().size();
							int training_proportion = (int) Math.ceil((this.partitions[0] * total_instances) / 100.0f);
							int testing_proportion = (int) Math.ceil((this.partitions[1] * total_instances) / 100.0f);
							int[] partitions_ = new int[]{training_proportion, testing_proportion};
							int cursor = 0;
							while (Arrays.stream(partitions_).sum() > 0) {
								// ...until no partition wants any more
								if (partitions_[cursor] > 0) {
									Optional<Integer> id = this.getFirstElement(strata.getValue()); //select first comment_id from the stratum to have fix training and test split
									if (id.isPresent()) {
										// might have exhausted population
										this.removeSentence(strata.getValue(), id.get());
										insert.setInt(1, id.get());
										insert.setInt(2, cursor);
										insert.setString(3, aCategory);
										insert.executeUpdate();
									}
									// even if we did not get any, there is nothing more to get, pretend we took something
									partitions_[cursor] = partitions_[cursor] - 1;
								}
								// advance to next partition
								cursor = (cursor + 1) % partitions_.length;
							}

					}
				}
			}
		}
	}

	private void removeSentence(Set<Integer> strata, Integer id) {
		Iterator<Integer> iterator = strata.iterator();
		while (iterator.hasNext()) {
			Integer entry = iterator.next();
			if(entry == id){
				iterator.remove();
				break;
			}
		}
	}

	private <E> Optional<E> getFirstElement(Collection<E> e) {
		return e.stream().findFirst();
	}
}
