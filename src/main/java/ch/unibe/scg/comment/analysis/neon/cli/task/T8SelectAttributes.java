package ch.unibe.scg.comment.analysis.neon.cli.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import weka.attributeSelection.AttributeSelection;
import weka.attributeSelection.InfoGainAttributeEval;
import weka.attributeSelection.Ranker;
import weka.core.Instances;
import weka.core.converters.ArffLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Select features/attributes based on information gain threshold
 * Ranker: Ranks attributes by their individual evaluations
 * Evaluator: InfoGainAttributeEval, evaluates the worth of an attribute by measuring the information gain with
 * respect to the class
 */
public class T8SelectAttributes {

	private static final Logger LOGGER = LoggerFactory.getLogger(T8SelectAttributes.class);
	private final String data;
	private final Path directory;

	public T8SelectAttributes( String data, Path directory) {
		this.data = data;
		this.directory =  directory;
	}

	public void run() throws SQLException, IOException {

			Files.list(this.directory).filter(
					p -> p.getFileName().toString().endsWith(".arff")
					&& p.getFileName().toString().startsWith("0-0-")
					&& !p.getFileName().toString().contains("tfidf"))
					.forEach(p -> {
						String training = p.getFileName().toString().split("\\.")[0];
						try{
							// training
							ArffLoader trainingLoader = new ArffLoader();
							trainingLoader.setFile(p.toFile());
							Instances trainingInstances = trainingLoader.getDataSet();
							trainingInstances.setClassIndex(trainingInstances.numAttributes() - 1);


							InfoGainAttributeEval infoGainAttributeEval = new InfoGainAttributeEval(); //Evaluator
							Ranker search = new Ranker(); //Ranker
							search.setOptions( new String[] {"-T", "0.005"}); //information gain threshold
							AttributeSelection attributeSelection = new AttributeSelection();
							attributeSelection.setEvaluator(infoGainAttributeEval);
							attributeSelection.setSearch(search);

							//apply attribute selection
							attributeSelection.SelectAttributes(trainingInstances);
							System.out.println(attributeSelection.toResultsString());

/*							double[][] rankedAttributes =  attributeSelection.rankedAttributes();
							for (int i = 0;i < rankedAttributes.length;i++ ){
									System.out.println("Attribute : "+ rankedAttributes[i][0]+" with Rank "+rankedAttributes[i][1]);
							}*/

							//save results
							String prefix =  p.getFileName().toString().split("\\.")[0];
							String[] parts = prefix.split("-");
							String category = parts[2];
							Path attributeSelectionResult = Files.createFile(this.directory.resolve(String.format("attributeSelection-%s.txt", category)));
							Files.writeString(attributeSelectionResult,attributeSelection.toResultsString());


						}catch (Throwable e) {
							LOGGER.warn("{} build classifiers {} failed", this.data, training, e);}
					});

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
