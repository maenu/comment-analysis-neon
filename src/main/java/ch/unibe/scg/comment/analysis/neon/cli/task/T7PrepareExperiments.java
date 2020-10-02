package ch.unibe.scg.comment.analysis.neon.cli.task;

import ch.unibe.scg.comment.analysis.neon.cli.InstancesBuilder;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.RemoveByName;
import weka.filters.unsupervised.attribute.Reorder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Prepare the dataset, generate arff files from the dataset and store them
 * @datbase input database (sqlite for now)
 * @data language under analysis
 * @directory directory of the dataset where arff files and other intermediate data can be saved
 */
public class T7PrepareExperiments {

	private final String database;
	private final String data;
	private final Path directory;

	public T7PrepareExperiments(String database, String data, Path directory) {
		super();
		this.database = database;
		this.data = data;
		this.directory = directory;
	}

	public void run() throws Exception {
		this.directory.toFile().mkdirs();
		try (
				Connection connection = DriverManager.getConnection("jdbc:sqlite:" + this.database);
				Statement statement = connection.createStatement()
		) {
			statement.executeUpdate("PRAGMA foreign_keys = on");
			try (
					ResultSet result = statement.executeQuery(
							"SELECT partition, extractors_partition, dataset FROM " + this.data + "_6_dataset")
			) {
				while (result.next()) {
					int partition = result.getInt("partition");
					int extractorsPartition = result.getInt("extractors_partition");
					byte[] dataset = result.getBytes("dataset");
					Instances instances = InstancesBuilder.load(dataset);
					List<String> categoryAttributeNames = new ArrayList<>();
					for (int i = 0; i < instances.numAttributes(); i++) {
						String name = instances.attribute(i).name();
						if (name.startsWith("category-")) {
							categoryAttributeNames.add(name);
						}
					}
					//for each category, prepare and generate arff files for each feature set (tfidf, heuristic, and both)
					for (String categoryAttributeName : categoryAttributeNames) {
						this.storeDataset(instances,
								partition,
								extractorsPartition,
								categoryAttributeName,
								true,
								true
						);
						this.storeDataset(instances,
								partition,
								extractorsPartition,
								categoryAttributeName,
								true,
								false
						);
						this.storeDataset(instances,
								partition,
								extractorsPartition,
								categoryAttributeName,
								false,
								true
						);
					}
				}
			}
		}
	}

	private void storeDataset(
			Instances instances,
			int partition,
			int extractorsPartition,
			String categoryAttributeName,
			boolean tfidf,
			boolean heuristic
	) throws Exception {
		// prepare dataset
		String postfix = "";
		if (tfidf) {
			postfix = postfix + "-tfidf";
		}
		if (heuristic) {
			postfix = postfix + "-heuristic";
		}
		String prefix = String.format(
				"%d-%d-%s%s",
				partition,
				extractorsPartition,
				categoryAttributeName.substring("category-".length()).toLowerCase().replaceAll("[^a-z0-9]", ""),
				postfix
		);
		Instances copy = this.prepareDataset(instances, categoryAttributeName, tfidf, heuristic);
		copy.setRelationName(prefix);
		// save dataset
		ArffSaver saver = new ArffSaver();
		Path path = Files.createFile(this.directory.resolve(String.format("%s.arff", prefix)));
		saver.setFile(path.toFile());
		saver.setInstances(copy);
		saver.writeBatch();
	}

	private Instances prepareDataset(
			Instances instances, String categoryAttributeName, boolean tfidf, boolean heuristic
	) throws Exception {
		if (!tfidf && !heuristic) {
			throw new IllegalArgumentException(
					"tfidf and heuristic cannot be both false, there would be no features left otherwise");
		}
		// copy it, as we are going to mess with that one for sure
		instances = new Instances(instances);
		// remove other categories
		for (int i = instances.numAttributes() - 1; i >= 0; i = i - 1) {
			String name = instances.attribute(i).name();
			if (name.startsWith("category-") && !name.equals(categoryAttributeName)) {
				instances.deleteAttributeAt(i);
			}
		}
		// reorder to have label last, as it is default in weka
		Reorder reorder = new Reorder();
		reorder.setAttributeIndices("2-last,first");
		reorder.setInputFormat(instances);
		instances = Filter.useFilter(instances, reorder);
		// remove attributes as desired
		if (!tfidf) {
			RemoveByName remove = new RemoveByName();
			remove.setExpression("^tfidf-.*$");
			remove.setInputFormat(instances);
			instances = Filter.useFilter(instances, remove);
		}
		if (!heuristic) {
			RemoveByName remove = new RemoveByName();
			remove.setExpression("^heuristic-.*$");
			remove.setInputFormat(instances);
			instances = Filter.useFilter(instances, remove);
		}
		return instances;
	}

}
