package ch.unibe.scg.comment.analysis.neon.cli;

import ch.unibe.scg.comment.analysis.neon.cli.task.*;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String args[]) throws Exception {
        CommandLineParser parser = new DefaultParser();
        Options options = new Options();
        options.addOption(Option.builder("D")
                .longOpt("database")
                .required()
                .hasArg()
                .desc("database path")
                .build());
        options.addOption(Option.builder("d")
                .longOpt("data")
                .required()
                .hasArg()
                .desc("data source [pharo]")
                .build());
        options.addOption(Option.builder("t")
                .longOpt("task")
                .required()
                .hasArgs()
                .valueSeparator(',')
                .desc("task to perform, split by ',', [preprocess|split-sentences|map-sentences|partition|extract-heuristics]")
                .build());
        try {
            CommandLine line = parser.parse(options, args);
            String database = line.getOptionValue("database");
            String data = line.getOptionValue("data");
            for (String task : line.getOptionValues("task")) {
                LOGGER.info("Running {}...", task);
                if ("preprocess".equals(task)) {
                    (new Preprocess(database, data)).run();
                } else if ("split-sentences".equals(task)) {
                    (new SplitSentences(database, data)).run();
                } else if ("map-sentences".equals(task)) {
                    (new MapSentences(database, data)).run();
                } else if ("partition".equals(task)) {
                    (new Partition(database, data, 3)).run();
                } else if ("extract-heuristics".equals(task)) {
                    (new ExtractHeuristics(database, data)).run();
                } else {
                    throw new IllegalArgumentException("task option is unknown");
                }
            }
        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar THIS.jar", options);
        }
    }

}
