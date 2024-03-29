package ch.unibe.scg.comment.analysis.neon.cli;

import ch.unibe.scg.comment.analysis.neon.cli.task.T10BuildClassifiers;
import ch.unibe.scg.comment.analysis.neon.cli.task.T11ImportClassifierOutputs;
import ch.unibe.scg.comment.analysis.neon.cli.task.T1Preprocess;
import ch.unibe.scg.comment.analysis.neon.cli.task.T2SplitSentences;
import ch.unibe.scg.comment.analysis.neon.cli.task.T3MapSentences;
import ch.unibe.scg.comment.analysis.neon.cli.task.T4PartitionSentences;
import ch.unibe.scg.comment.analysis.neon.cli.task.T5PrepareExtractors;
import ch.unibe.scg.comment.analysis.neon.cli.task.T6PrepareDatasets;
import ch.unibe.scg.comment.analysis.neon.cli.task.T7PrepareExperiments;
import ch.unibe.scg.comment.analysis.neon.cli.task.T8RunExperiments;
import ch.unibe.scg.comment.analysis.neon.cli.task.T9ImportExperimentResults;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;

public class Main {

	private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

	public static void main(String[] args) throws Exception {
		CommandLineParser parser = new DefaultParser();
		Options options = new Options();
		options.addOption(Option.builder("D").longOpt("database").required().hasArg().desc("database path").build());
		options.addOption(Option.builder("d")
				.longOpt("data")
				.required()
				.hasArgs()
				.valueSeparator(',')
				.desc("data source [pharo|java|python]")
				.build());
		options.addOption(Option.builder("t")
				.longOpt("task")
				.required()
				.hasArgs()
				.valueSeparator(',')
				.desc("task to perform, split by ',', [preprocess|split-sentences|map-sentences|partition|prepare-extractors|prepare-datasets]")
				.build());
		try {
			CommandLine line = parser.parse(options, args);
			String database = line.getOptionValue("database");
			for (String task : line.getOptionValues("task")) {
				for (String data : line.getOptionValues("data")) {
					LOGGER.info("Running {} on {}...", task, data);
					if ("1-preprocess".equals(task)) {
						(new T1Preprocess(database, data)).run();
					} else if ("2-split-sentences".equals(task)) {
						(new T2SplitSentences(database, data)).run();
					} else if ("3-map-sentences".equals(task)) {
						(new T3MapSentences(database, data)).run();
					} else if ("4-partition-sentences".equals(task)) {
						(new T4PartitionSentences(database, data, new int[]{3, 1})).run();
					} else if ("5-prepare-extractors".equals(task)) {
						(new T5PrepareExtractors(database, data, 1000)).run();
					} else if ("6-prepare-datasets".equals(task)) {
						(new T6PrepareDatasets(database, data, 0)).run();
					} else if ("7-prepare-experiments".equals(task)) {
						(new T7PrepareExperiments(
								database,
								data,
								Paths.get(System.getProperty("user.dir"))
										.resolve("data")
										.resolve(data)
										.resolve("experiment")
						)).run();
					} else if ("8-run-experiments".equals(task)) {
						(new T8RunExperiments(
								data,
								Paths.get(System.getProperty("user.dir"))
										.resolve("data")
										.resolve(data)
										.resolve("experiment"),
								64
						)).run();
					} else if ("9-import-experiment-results".equals(task)) {
						(new T9ImportExperimentResults(
								database,
								data,
								Paths.get(System.getProperty("user.dir"))
										.resolve("data")
										.resolve(data)
										.resolve("experiment")
						)).run();
					} else if ("10-build-classifiers".equals(task)) {
						(new T10BuildClassifiers(
								data,
								Paths.get(System.getProperty("user.dir"))
										.resolve("data")
										.resolve(data)
										.resolve("experiment"),
								64
						)).run();
					} else if ("11-import-classifier-outputs".equals(task)) {
						(new T11ImportClassifierOutputs(
								database,
								data,
								Paths.get(System.getProperty("user.dir"))
										.resolve("data")
										.resolve(data)
										.resolve("experiment")
						)).run();
					} else {
						throw new IllegalArgumentException("task option is unknown");
					}
				}
			}
		} catch (ParseException | IllegalArgumentException e) {
			System.err.println(e.getMessage());
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("java -jar THIS.jar", options);
		}
	}

}
