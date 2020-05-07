package ch.unibe.scg.comment.analysis.neon.cli.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import weka.experiment.Experiment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RunExperiments {

	private static final Logger LOGGER = LoggerFactory.getLogger(RunExperiments.class);
	private final String data;
	private final Path directory;
	private final int threads;
	private final String experimentConfigurationTemplate;

	public RunExperiments(String data, Path directory, int threads) throws IOException {
		super();
		this.data = data;
		this.directory = directory;
		this.threads = threads;
		this.experimentConfigurationTemplate = Files.readString(Paths.get(PrepareExperiments.class.getClassLoader()
				.getResource("training-configuration.xml")
				.getFile()));
	}

	public void run() throws IOException, InterruptedException {
		ExecutorService executor = Executors.newFixedThreadPool(this.threads);
		Files.list(this.directory)
				.filter(p -> p.getFileName().toString().endsWith(".arff") && p.getFileName()
						.toString()
						.startsWith("0-0-"))
				.forEach(p -> {
					String prefix = p.getFileName().toString().split("\\.")[0];
					try {
						// save experimenter configuration
						Path configuration = this.directory.resolve(Paths.get(String.format("%s.xml", prefix)));
						Files.writeString(configuration,
								this.experimentConfigurationTemplate.replaceAll("\\{\\{prefix}}",
										this.directory.resolve(prefix).toAbsolutePath().toString()
								)
										.replaceAll("\\{\\{numSlots}}", "1")
						);
						executor.submit(() -> {
							LOGGER.info("{} training {}...", this.data, prefix);
							Experiment.main(new String[]{"-l", configuration.toFile().getAbsolutePath(), "-r"});
						});
					} catch (IOException e) {
						e.printStackTrace();
					}
				});
		executor.awaitTermination(5, TimeUnit.HOURS);
		executor.shutdown();
	}

}
