package ch.unibe.scg.comment.analysis.neon.cli;

import org.apache.commons.lang3.StringUtils;
import weka.core.stopwords.NltkStopwords;

public class Tempclass {
	public static void main(String[] args) throws Exception {
		String s = "@noextend This class camelCaseSpliiter since 1.2 is not intended to be subclassed by clients.";
		String processedSentence = preprocess(s);
		System.out.println(processedSentence);
		System.out.println(new NltkStopwords().loadStopwords());
	}

	public static String preprocess(String s) {
		if (s == null) {
			return null;
		}

		StringBuffer sb= new StringBuffer("");
		String[] words = s.split("\\s+");
		for (String word: words){
			String[] newWords = word.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])");
			//sb.append(StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(word),StringUtils.SPACE));
			sb.append(StringUtils.join(newWords,StringUtils.SPACE));
			sb.append(StringUtils.SPACE);
		}
		s = sb.toString().trim();

		return s.toLowerCase().replaceAll("\r\n|\r", "\n") // reduce line endings -> line endings are now \n
				.replaceAll("[^a-z0-9,.!?@ \n]", " ") // reduce alphabet -> string is now [a-z0-9,.!?@ \n]+
				.replaceAll("([0-9]+)\\.([0-9]+)", "$1$2") // replace floats
				.replaceAll("\\d+"," ") //remove numbers
				.replaceAll(" +", " ") // reduce spaces -> spaces are now single spaces
				.replaceAll("^[ \n]+", "") // reduce document start -> document does not start with whitespace
				.replaceAll("[ \n]+$", "") // reduce document end -> document does not end with whitespace
				.replaceAll("\n ", "\n") // reduce line starts -> lines do not start with spaces
				.replaceAll(" \n", "\n") // reduce line ends -> lines do not end with spaces
				.replaceAll("\n(\n+)", "\n\n") // reduce newlines -> line ends are now \n or \n\n
				.replaceAll("([^.!?\n])\n([^\n])", "$1 $2") // join non-sentence lines -> whole sentence on same line
				.replaceAll(" ?([.!?])[ .!?]*", "$1") // normalize sentence ends -> multi-terminated is now single-terminated
				.replaceAll(" ?,[ ,]*", ", ") // normalize sentence ends -> multi-terminated is now single-terminated
				.replaceAll("([,.!?])([^ ])", "$1 $2") // split joined sentences -> sentences and parts are separated by space
				.replaceAll("(\n|^)[ .!?]+", "$1") // ensure line starts with non-separators
				.replaceAll("(^|[^a-z0-9])e\\.? ?g\\.? ?($|[^.a-z0-9])", "$1eg$2") // replace eg
				.replaceAll("(^|[^a-z0-9])i\\.? ?e\\.? ?($|[^.a-z0-9])", "$1ie$2") // replace ie
				.replaceAll("(^|[^a-z0-9])etc\\.? ?($|[^.a-z0-9])", "$1etc$2") // replace etc
				//.replaceAll("[^.!?\n]{400}[^ \n]*[ \n]?", "$0\n\n") // split long sentences -> sentences are now at most 400 characters plus one word long
				.trim();
	}

}
