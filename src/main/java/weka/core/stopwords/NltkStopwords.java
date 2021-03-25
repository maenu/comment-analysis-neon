package weka.core.stopwords;

import ch.unibe.scg.comment.analysis.neon.cli.task.Utility;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;

public class NltkStopwords extends AbstractStopwords {
	protected HashSet<String> m_Words;
	/**
	 * Returns a string describing the stopwords scheme.
	 *
	 * @return a description suitable for displaying in the gui
	 */
	@Override
	public String globalInfo() {
		return
				"Stopwords list based on NLTK:\n"
						+ "https://github.com/igorbrigadir/stopwords";
	}

	public HashSet<String> loadStopwords() throws IOException {
		String line;
		HashSet<String> stopwords = new HashSet<String>();
		 File file = new File(Utility.class.getClassLoader().getResource("nltk-stopwords.txt").getFile());
		FileReader fileReader = new FileReader(file);
		BufferedReader bufferedReader = new BufferedReader(fileReader);
		while((line = bufferedReader.readLine())!=null){
			stopwords.add(line);
		}
		return stopwords;
	}

	@Override
	protected void initialize() {
		super.initialize();
		m_Words = new HashSet<String>();

		try {
			m_Words =  this.loadStopwords();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Returns true if the given string is a stop word.
	 *
	 * @param word the word to test
	 * @return true if the word is a stopword
	 */
	@Override
	protected boolean is(String word) {
		return m_Words.contains(word.trim().toLowerCase());
	}

}
