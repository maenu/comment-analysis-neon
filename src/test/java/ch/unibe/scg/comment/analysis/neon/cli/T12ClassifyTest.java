package ch.unibe.scg.comment.analysis.neon.cli;

import ch.unibe.scg.comment.analysis.neon.cli.NormalizedString.Range;
import ch.unibe.scg.comment.analysis.neon.cli.task.T12Classify;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class T12ClassifyTest {

	final String original = """
			The String class represents character strings. All string literals in Java 1.6 programs, such as "abc", are implemented as instances of this class.
			Strings are constant; their values cannot be changed after they are created. String buffers support mutable strings. Because String objects are immutable they can be shared. For example:
			       String str = "abc";
			  
			is equivalent to:
			       char data[] = {'a', 'b', 'c'};
			       String str = new String(data);
			  
			Here are some more examples of how strings can be used:
			       System.out.println("abc");
			       String cde = "cde";
			       System.out.println("abc" + cde);
			       String c = "abc".substring(2, 3);
			       String d = cde.substring(1, 2);
			  
			The class String includes methods for examining individual characters of the sequence, for comparing strings, for searching strings, for extracting substrings, and for creating a copy of a string with all characters translated to uppercase or to lowercase. Case mapping is based on the Unicode Standard version specified by the Character class.
			""";
	final NormalizedString normalized = new NormalizedString(this.original);

	@Test
	public void rangesInNormalized() throws Exception {
		T12Classify task = new T12Classify("java");
		List<Range> ranges = task.rangesInNormalized(this.normalized);
		assertEquals(ranges.size(), 16);
		for (Range range : ranges) {
			assertTrue(range.length() > 0);
		}
		assertEquals(
				String.join("\n",
						ranges.stream().map(range -> range.in(this.normalized.normalized)).collect(Collectors.toList())
				),
				"""
						the string class represents character strings.
						all string literals in java 16 programs, such as abc , are implemented as instances of this class.
						strings are constant their values cannot be changed after they are created.
						string buffers support mutable strings.
						because string objects are immutable they can be shared.
						for example string str abc
						is equivalent to char data a , b , c string str new string data
						here are some more examples of how strings can be used system.
						out.
						println abc string cde cde system.
						out.
						println abc cde string c abc .
						substring 2, 3 string d cde.
						substring 1, 2
						the class string includes methods for examining individual characters of the sequence, for comparing strings, for searching strings, for extracting substrings, and for creating a copy of a string with all characters translated to uppercase or to lowercase.
						case mapping is based on the unicode standard version specified by the character class."""
		);
	}

	@Test
	public void rangesInOriginal() throws Exception {
		T12Classify task = new T12Classify("java");
		List<Range> rangesNormalized = task.rangesInNormalized(this.normalized);
		List<Range> rangesOriginal = task.rangesInOriginal(this.normalized, rangesNormalized);
		assertEquals(rangesOriginal.size(), rangesNormalized.size());
		for (Range range : rangesOriginal) {
			assertTrue(range.length() > 0);
		}
		assertEquals(String.join("\n",
				rangesOriginal.stream().map(range -> range.in(this.original)).collect(Collectors.toList())
		), """
				The String class represents character strings.\s
				All string literals in Java 1.6 programs, such as "abc", are implemented as instances of this class.
				Strings are constant; their values cannot be changed after they are created.\s
				String buffers support mutable strings.\s
				Because String objects are immutable they can be shared.\s
				For example:
				       String str = "abc
				is equivalent to:
				       char data[] = {'a', 'b', 'c'};
				       String str = new String(data
				Here are some more examples of how strings can be used:
				       System.
				out.
				println("abc");
				       String cde = "cde";
				       System.
				out.
				println("abc" + cde);
				       String c = "abc".
				substring(2, 3);
				       String d = cde.
				substring(1, 2
				The class String includes methods for examining individual characters of the sequence, for comparing strings, for searching strings, for extracting substrings, and for creating a copy of a string with all characters translated to uppercase or to lowercase.\s
				Case mapping is based on the Unicode Standard version specified by the Character class.""");
	}

	@Test
	public void classify() throws Exception {
		T12Classify task = new T12Classify("java");
		Map<Range, List<String>> result = task.classify(this.original);
		assertEquals(result.size(), 16);
		assertEquals(result.values().stream().map(Collection::size).reduce(Integer::sum).get(), 9);
	}

	@Test
	public void label() throws Exception {
		String source = Files.readString(Paths.get(
				"src/main/java/ch/unibe/scg/comment/analysis/neon/cli/StringToHeuristicVector.java"));
		T12Classify task = new T12Classify("java");
		Map<Range, List<String>> result = task.label(source);
		assertEquals(String.join("\n",
				result.keySet().stream().map(range -> range.in(source)).collect(Collectors.toList())
		), """
				attributes
				class
				return "heuristic-[category]-[heuristic
				Finds the category matching the heuristic class.\s
				As NEON processes labels, normalization is required.
				param heuristicClass
					 * @return""");
		assertEquals(result.size(), 6);
		assertEquals(result.values().stream().map(Collection::size).reduce(Integer::sum).get(), 5);
	}

}
