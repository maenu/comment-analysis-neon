package ch.unibe.scg.comment.analysis.neon.cli.task;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Utility {

	public static String resource(String path) throws IOException {
		return Files.readString(Paths.get(T7PrepareExperiments.class.getClassLoader().getResource(path).getFile()));
	}

}
