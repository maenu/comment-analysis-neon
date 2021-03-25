package ch.unibe.scg.comment.analysis.neon.cli;

import org.apache.commons.lang3.StringUtils;
import org.tartarus.snowball.ext.PorterStemmer;
import weka.core.Attribute;
import weka.core.DictionaryBuilder;
import weka.core.Instances;
import weka.core.SparseInstance;
import weka.core.converters.ArffLoader;
import weka.core.converters.ArffSaver;
import weka.core.stemmers.IteratedLovinsStemmer;
import weka.core.stemmers.SnowballStemmer;
import weka.core.stemmers.Stemmer;
import weka.core.stopwords.NltkStopwords;
import weka.core.stopwords.Rainbow;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.FixedDictionaryStringToWordVector;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Creates the instances (dataset) and prepare the arff files
 * @dictionary features from tfidif
 * @categories list of categories
 */
public class InstancesBuilder {

	private final List<String> categories;
	private final File dictionary;
	private final Instances instances;

	public InstancesBuilder(String name, List<String> categories, File dictionary) {
		super();
		this.categories = categories;
		this.dictionary = dictionary;
		ArrayList<Attribute> attributes = new ArrayList<>();
		// add category labels first
		for (String category : this.categories) {
			attributes.add(new Attribute(this.categoryName(category), List.of("0", "1")));
		}
		// add text attribute last
		attributes.add(new Attribute("text", true, null));
		this.instances = new Instances(name, attributes, 0);
	}

	public static Instances load(byte[] bytes) throws IOException {
		ArffLoader loader = new ArffLoader();
		Path path = Files.createTempFile("dataset", ".arff");
		Files.write(path, bytes);
		loader.setFile(path.toFile());
		return loader.getDataSet();
	}

	public static byte[] save(Instances instances) throws IOException {
		ArffSaver saver = new ArffSaver();
		Path path = Files.createTempFile("dataset", ".arff");
		saver.setFile(path.toFile());
		saver.setInstances(instances);
		saver.writeBatch();
		return Files.readAllBytes(path);
	}

	/** Preprocess the comment.
	 * Allow only letter and digit, remove all special symbols, and atmost 400 characters in a sentence.
	 *
	 * @param s the comment to preprocess
	 * @return preprocessed sentence
	 */
	public static String preprocess(String s) {
		if (s == null) {
			return null;
		}

		StringBuffer sb= new StringBuffer("");
		String[] words = s.split("\\s+");
		for (String word: words){
			String[] newWords = word.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])");		//split camel cases
			//sb.append(StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(word),StringUtils.SPACE)); //creates problems with version number 1.2
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

	public void add(String sentence, Set<String> categories) {
		SparseInstance instance = new SparseInstance(this.instances.numAttributes());
		instance.setDataset(this.instances);
		for (String category : this.categories) {
			instance.setValue(this.instances.attribute(this.categoryName(category)),
					categories.contains(category) ? "1" : "0"
			);
		}
		instance.setValue(this.instances.attribute("text"), preprocess(sentence));
		this.instances.add(instance);
	}

	public Instances build() throws Exception {
		Instances instances = this.tfidf(this.instances); //adding tfidf features
		instances.setRelationName(this.instances.relationName());
		return instances;
	}

	private String categoryName(String category) {
		return String.format("category-%s", category);
	}



	/**
	 * Transforms instances from text to TF-IDF features.
	 *
	 * @param instances Must have a "text" attribute
	 * @return
	 * @throws Exception
	 */
	private Instances tfidf(Instances instances) throws Exception {
		int i = instances.attribute("text").index() + 1;
		FixedDictionaryStringToWordVector filter = new FixedDictionaryStringToWordVector();
		filter.setLowerCaseTokens(true);
		//filter.setStopwordsHandler(new Rainbow());
		filter.setStopwordsHandler(new NltkStopwords()); //stopwords list based on NLTK https://github.com/igorbrigadir/stopwords
		filter.setStemmer(new SnowballStemmer());
		filter.setOutputWordCounts(true);
		filter.setTFTransform(true);
		filter.setIDFTransform(true);
		filter.setAttributeNamePrefix("tfidf-");
		filter.setAttributeIndices(String.format("%d-%d", i, i));
		filter.setDictionaryFile(this.dictionary);
		filter.setInputFormat(instances);
		// fix broken m_count in dictionary build, any positive constant will work
		Field mCount = DictionaryBuilder.class.getDeclaredField("m_count");
		mCount.setAccessible(true);
		Field mVectorizer = FixedDictionaryStringToWordVector.class.getDeclaredField("m_vectorizer");
		mVectorizer.setAccessible(true);
		mCount.set(mVectorizer.get(filter), 1000);
		return Filter.useFilter(instances, filter);
	}

}
