package ch.unibe.scg.comment.analysis.neon.cli.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.rules.OneR;
import weka.classifiers.rules.ZeroR;
import weka.classifiers.trees.J48;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.core.converters.ArffLoader;
import weka.filters.Filter;
import weka.filters.supervised.instance.ClassBalancer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class BuildClassifiers {

	private static final Logger LOGGER = LoggerFactory.getLogger(BuildClassifiers.class);
	private final String data;
	private final Path directory;
	private final int threads;

	public BuildClassifiers(String data, Path directory, int threads) {
		super();
		this.data = data;
		this.directory = directory;
		this.threads = threads;
	}

	public void run() throws IOException, InterruptedException {
		ExecutorService executor = Executors.newFixedThreadPool(this.threads);
		Files.list(this.directory)
				.filter(p -> p.getFileName().toString().endsWith(".arff") && p.getFileName()
						.toString()
						.startsWith("0-0-"))
				.forEach(p -> {
					String training = p.getFileName().toString().split("\\.")[0];
					String test = training.replaceAll("^0-0-", "1-0-");
					executor.submit(() -> {
						LOGGER.info("{} build classifiers {}...", this.data, training);
						try {
							// training
							ArffLoader trainingLoader = new ArffLoader();
							trainingLoader.setFile(p.toFile());
							Instances trainingInstances = trainingLoader.getDataSet();
							trainingInstances.setClassIndex(trainingInstances.numAttributes() - 1);
							// test
							ArffLoader testLoader = new ArffLoader();
							testLoader.setFile(p.toFile());
							Instances testInstances = testLoader.getDataSet();
							testInstances.setClassIndex(testInstances.numAttributes() - 1);
							// zero rule
							this.trainAndTest(
									new ZeroR(),
									training,
									"zero-r",
									new Instances(trainingInstances),
									new Instances(testInstances)
							);
							// one rule
							this.trainAndTest(
									new OneR(),
									training,
									"one-r",
									new Instances(trainingInstances),
									new Instances(testInstances)
							);
							// naive bayes
							this.trainAndTest(
									new NaiveBayes(),
									training,
									"naive-bayes",
									this.balance(trainingInstances),
									new Instances(testInstances)
							);
							// j48
							this.trainAndTest(
									new J48(),
									training,
									"j48",
									this.balance(trainingInstances),
									new Instances(testInstances)
							);
							// random forest
							this.trainAndTest(
									new RandomForest(),
									training,
									"random-forest",
									this.balance(trainingInstances),
									new Instances(testInstances)
							);
						} catch (Exception e) {
							e.printStackTrace();
						}
					});
				});
		executor.awaitTermination(5, TimeUnit.HOURS);
		executor.shutdown();
	}

	private void trainAndTest(
			Classifier classifier, String prefix, String postfix, Instances trainingInstances, Instances testInstances
	) throws Exception {
		classifier.buildClassifier(trainingInstances);
		SerializationHelper.write(this.directory.resolve(String.format("%s-%s.classifier", prefix, postfix))
				.toAbsolutePath()
				.toString(), classifier);
		String output = "dataset,TP,FP,TN,FN\n";
		Evaluation evaluation = new Evaluation(trainingInstances);
		evaluation.evaluateModel(classifier, trainingInstances);
		output = String.format(
				"%straining,%f,%f,%f,%f\n",
				output,
				evaluation.numTruePositives(1),
				evaluation.numFalsePositives(1),
				evaluation.numTrueNegatives(1),
				evaluation.numFalseNegatives(1)
		);
		evaluation = new Evaluation(trainingInstances);
		evaluation.evaluateModel(classifier, testInstances);
		output = String.format(
				"%stest,%f,%f,%f,%f\n",
				output,
				evaluation.numTruePositives(1),
				evaluation.numFalsePositives(1),
				evaluation.numTrueNegatives(1),
				evaluation.numFalseNegatives(1)
		);
		Files.writeString(this.directory.resolve(String.format("%s-%s-output.csv", prefix, postfix)), output);
	}

	private Instances balance(Instances instances) throws Exception {
		ClassBalancer classBalancer = new ClassBalancer();
		classBalancer.setInputFormat(instances);
		instances = Filter.useFilter(instances, classBalancer);
		return instances;
	}

}
