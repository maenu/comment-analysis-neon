package ch.unibe.scg.comment.analysis.neon.cli.task;

import ch.unibe.scg.comment.analysis.neon.cli.InstancesBuilder;
import weka.core.converters.ArffSaver;

import java.io.ByteArrayOutputStream;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PrepareDatasets {

	private final String database;
	private final String data;
	private final int extractorsPartition;

	public PrepareDatasets(String database, String data, int extractorsPartition) {
		super();
		this.database = database;
		this.data = data;
		this.extractorsPartition = extractorsPartition;
	}

	public void run() throws Exception {
		try (
				Connection connection = DriverManager.getConnection("jdbc:sqlite:" + this.database);
				Statement statement = connection.createStatement();
				PreparedStatement insert = connection.prepareStatement("INSERT INTO " + this.data
						+ "_dataset (partition, extractors_partition, dataset) VALUES (?, ?, ?)")
		) {
			statement.executeUpdate("PRAGMA foreign_keys = on");
			this.checkPrecondition(statement);
			List<String> categories = this.categories(statement);
			Map<Integer, String> sentences = new HashMap<>();
			Map<Integer, Map<Integer, Set<String>>> partitions = new HashMap<>();
			try (
					ResultSet result = statement.executeQuery("SELECT * FROM " + this.data + "_ground_truth")
			) {
				while (result.next()) {
					int partition = result.getInt("partition");
					int id = result.getInt("comment_sentence_id");
					String sentence = result.getString("comment_sentence");
					String category = result.getString("category");
					sentences.put(id, sentence);
					if (!partitions.containsKey(partition)) {
						partitions.put(partition, new HashMap<>());
					}
					if (!partitions.get(partition).containsKey(id)) {
						partitions.get(partition).put(id, new HashSet<>());
					}
					partitions.get(partition).get(id).add(category);
				}
			}
			for (Map.Entry<Integer, Map<Integer, Set<String>>> partition : partitions.entrySet()) {
				InstancesBuilder builder = this.instancesBuilder(statement, categories);
				for (Map.Entry<Integer, Set<String>> sentence : partition.getValue().entrySet()) {
					builder.add(sentences.get(sentence.getKey()), sentence.getValue());
				}
				try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
					ArffSaver saver = new ArffSaver();
					saver.setInstances(builder.build());
					saver.setDestination(buffer);
					saver.writeBatch();
					insert.setInt(1, partition.getKey());
					insert.setInt(2, this.extractorsPartition);
					insert.setBytes(3, buffer.toByteArray());
					insert.executeUpdate();
				}
			}
		}
	}

	private InstancesBuilder instancesBuilder(
			Statement statement, List<String> categories
	) throws SQLException, IOException {
		try (
				ResultSet result = statement.executeQuery(
						"SELECT heuristics, dictionary FROM " + this.data + "_extractors WHERE partition = "
								+ this.extractorsPartition + "")
		) {
			result.next();
			Path heuristics = Files.createTempFile("heuristics", ".xml");
			Files.write(heuristics, result.getBytes("heuristics"));
			Path dictionary = Files.createTempFile("dictionary", ".csv");
			Files.write(dictionary, result.getBytes("dictionary"));
			return new InstancesBuilder(
					String.format("data-%d", this.extractorsPartition),
					categories,
					heuristics.toFile(),
					dictionary.toFile()
			);
		}
	}

	private List<String> categories(Statement statement) throws SQLException {
		List<String> categories = new ArrayList<>();
		try (
				ResultSet result = statement.executeQuery(
						"SELECT name FROM PRAGMA_TABLE_INFO('" + this.data + "_preprocessed') ORDER BY name ASC")
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
		try (ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM " + this.data + "_dataset")) {
			result.next();
			if (result.getInt(1) > 0) {
				throw new IllegalStateException(String.format("%s_dataset is not empty", this.data));
			}
		}
	}

}
