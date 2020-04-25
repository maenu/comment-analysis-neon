package ch.unibe.scg.comment.analysis.neon.cli;

import meka.core.MLUtils;
import weka.core.Attribute;
import weka.core.Instances;
import weka.core.SparseInstance;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.FixedDictionaryStringToWordVector;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class InstancesBuilder {

	public static final String[] TFIDF_FIXED_FILTER_OPTIONS = new String[]{
			"-P", "tfidf-", // attribute prefix
			"-L", // lowercase
			"-stemmer", "weka.core.stemmers.IteratedLovinsStemmer", // stemmer
			"-T", "true", // TF
			"-I", "true" // IDF
	};
	public static final String[] TFIDF_EXTRACT_FILTER_OPTIONS = new String[]{
			"-P", "tfidf-", // attribute prefix
			"-L", // lowercase
			"-stemmer", "weka.core.stemmers.IteratedLovinsStemmer", // stemmer
			"-T", // TF
			"-I" // IDF
	};
	private final List<String> categories;
	private final File heuristics;
	private final File dictionary;
	private final Instances instances;

	public InstancesBuilder(String name, List<String> categories, File heuristics, File dictionary) {
		super();
		this.categories = categories;
		this.heuristics = heuristics;
		this.dictionary = dictionary;
		ArrayList<Attribute> attributes = new ArrayList<>();
		// add category labels first
		for (String category : this.categories) {
			attributes.add(new Attribute(this.categoryName(category), List.of("0", "1")));
		}
		// add text attribute last
		attributes.add(new Attribute("text", true, null));
		// name with -C for MEKA
		this.instances = new Instances(String.format("%s: -C %d", name, this.categories.size()), attributes, 0);
	}

	public static String preprocess(String s) {
		if (s == null) {
			return null;
		}
		return s.toLowerCase().replaceAll("\r\n|\r", "\n") // reduce line endings -> line endings are now \n
				.replaceAll("[^a-z0-9,.!? \n]", " ") // reduce alphabet -> string is now [a-z0-9,.!? \n]+
				.replaceAll("([0-9]+)\\.([0-9]+)", "$1$2") // replace floats
				.replaceAll(" +", " ") // reduce spaces -> spaces are now single spaces
				.replaceAll("^[ \n]+", "") // reduce document start -> document does not start with whitespace
				.replaceAll("[ \n]+$", "") // reduce document end -> document does not end with whitespace
				.replaceAll("\n ", "\n") // reduce line starts -> lines do not start with spaces
				.replaceAll(" \n", "\n") // reduce line ends -> lines do not end with spaces
				.replaceAll("\n(\n+)", "\n\n") // reduce newlines -> line ends are now \n or \n\n
				.replaceAll("([^.!?\n])\n([^\n])", "$1 $2") // join non-sentence lines -> whole sentence on same line
				.replaceAll(" ?([.!?])[ .!?]*",
						"$1"
				) // normalize sentence ends -> multi-terminated is now single-terminated
				.replaceAll(" ?,[ ,]*", ", ") // normalize sentence ends -> multi-terminated is now single-terminated
				.replaceAll("([,.!?])([^ ])",
						"$1 $2"
				) // split joined sentences -> sentences and parts are separated by space
				.replaceAll("(\n|^)[ .!?]+", "$1") // ensure line starts with non-separators
				.replaceAll("(^|[^a-z0-9])e\\.? ?g\\.? ?($|[^.a-z0-9])", "$1eg$2") // replace eg
				.replaceAll("(^|[^a-z0-9])i\\.? ?e\\.? ?($|[^.a-z0-9])", "$1ie$2") // replace ie
				.replaceAll("(^|[^a-z0-9])etc\\.? ?($|[^.a-z0-9])", "$1etc$2") // replace etc
				.replaceAll("[^.!?\n]{400}[^ \n]*[ \n]?",
						"$0\n\n"
				) // split long sentences -> sentences are now at most 400 characters plus one word long
				.trim();
	}

	public void add(String sentence) {
		SparseInstance instance = new SparseInstance(this.instances.numAttributes());
		instance.setDataset(this.instances);
		for (String category : this.categories) {
			instance.setValue(this.instances.attribute(this.categoryName(category)), "0");
		}
		instance.setValue(this.instances.attribute("text"), preprocess(sentence));
		this.instances.add(instance);
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
		Instances instances = this.tfidf(this.heuristic(this.instances));
		instances.setRelationName(this.instances.relationName());
		MLUtils.prepareData(instances);
		return instances;
	}

	private String categoryName(String category) {
		return String.format("category-%s", category);
	}

	/**
	 * Transforms instances from text to heuristic features.
	 *
	 * @param instances Must have a "text" attribute
	 * @return
	 * @throws Exception
	 */
	private Instances heuristic(Instances instances) throws Exception {
		StringToHeuristicVector filter = new StringToHeuristicVector();
		filter.setCategories(this.categories);
		filter.setHeuristics(this.heuristics);
		filter.setInputFormat(instances);
		return Filter.useFilter(instances, filter);
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
		filter.setOptions(TFIDF_FIXED_FILTER_OPTIONS);
		filter.setAttributeIndices(String.format("%d-%d", i, i));
		filter.setDictionaryFile(this.dictionary);
		filter.setInputFormat(instances);
		return Filter.useFilter(instances, filter);
	}

}
