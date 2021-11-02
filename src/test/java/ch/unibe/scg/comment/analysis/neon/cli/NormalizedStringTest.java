package ch.unibe.scg.comment.analysis.neon.cli;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NormalizedStringTest {

	@Test
	public void range() {
		String original = """
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
		NormalizedString normalized = NormalizedString.normalize(original);
		assertEquals(normalized.getNormalized(), """
				the string class represents character strings. all string literals in java 16 programs, such as abc , are implemented as instances of this class.\s
				strings are constant their values cannot be changed after they are created. string buffers support mutable strings. because string objects are immutable they can be shared. for example string str abc
				    
				is equivalent to char data a , b , c string str new string data
				    
				here are some more examples of how strings can be used system. out. println abc string cde cde system. out. println abc cde string c abc . substring 2, 3 string d cde. substring 1, 2
				    
				the class string includes methods for examining individual characters of the sequence, for comparing strings, for searching strings, for extracting substrings, and for creating a copy of a string with all characters translated to uppercase or to lowercase. case mapping is based on the unicode standard version specified by the character class.""");
	}

	@Test
	public void normalizedToOriginal() {
		String original = "ksajd...asdf1.6flk; ajsl;;;;;;fj askdlj sakl..,.,3986asd ;k jkjlasdf ";
		NormalizedString normalized = NormalizedString.normalize(original);
		assertEquals(normalized.getNormalized(), "ksajd. asdf16flk ajsl fj askdlj sakl. , . , 3986asd k jkjlasdf");
		NormalizedString.Range normalizedRange = new NormalizedString.Range(3, 27);
		NormalizedString.Range originalRange = normalized.normalizedToOriginal(normalizedRange);
		assertEquals(originalRange, new NormalizedString.Range(3, 35));
		assertEquals(originalRange.in(original), "jd...asdf1.6flk; ajsl;;;;;;fj as");
		assertEquals(normalizedRange.in(normalized.getNormalized()), "jd. asdf16flk ajsl fj as");
	}

}
