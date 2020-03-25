package ch.unibe.scg.comment.analysis.neon.cli;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.neon.engine.Parser;
import org.neon.engine.XMLWriter;
import org.neon.model.Result;

public class Main {
    public static void main(String args[]) throws Exception {
        // Initialize the text that needs to be classified
        String text = Files.readString(Paths.get(args[0]));
        // The xml file containing the heuristics to use
        File heuristics = new File(args[1]);
        // The xml file that will contain the output of the classification
        File output = new File(args[2]);
        // The parser returns a collection of results
        ArrayList<Result>  results = Parser.getInstance().extract(text, heuristics);
        // Export the results in the destination file
        XMLWriter.addXMLSentences(output, results);
    }
}
