package ch.unibe.scg.comment.analysis.neon.cli.task;

import ch.unibe.scg.comment.analysis.neon.cli.InstancesBuilder;
import ch.unibe.scg.comment.analysis.neon.cli.NormalizedString;
import ch.unibe.scg.comment.analysis.neon.cli.NormalizedString.Range;
import org.neon.pathsFinder.engine.Parser;
import org.neon.pathsFinder.model.Sentence;
import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.Instances;
import weka.core.SerializationHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class T12Classify {

	public static void main(String[] args) throws Exception {
		String data = args[0];
		String source = Files.readString(Paths.get(args[1]));
		T12Classify task = new T12Classify(data);
		SortedMap<Range, List<String>> labels = task.label(source);
		System.out.println(labels);
	}

	private static final Map<String, Pattern> PATTERNS = Map.of(
			"java",
			Pattern.compile("//.*|(?s)/\\*.*?\\*/"),
			"pharo",
			Pattern.compile("(?s)(\"(?:\\\\[^\"]|\\\\\"|.)*?\")"),
			"python",
			Pattern.compile("#.*|(?s)(['\"])\1\1(.*?)\1{3}")
	);
	private final String data;
	private final File heuristics;
	private final File dictionary;
	private final List<String> categories;
	private final Map<String, Classifier> classifiers;
	private final Pattern commentPattern;

	public T12Classify(String data) throws Exception {
		this.data = data;
		this.heuristics = Files.createTempFile("heuristics", ".xml").toFile();
		try (OutputStream output = new FileOutputStream(this.heuristics)) {
			T12Classify.class.getClassLoader()
					.getResourceAsStream(String.format("classifier/%s/heuristics.xml", this.data))
					.transferTo(output);
		}
		this.dictionary = Files.createTempFile("dictionary", ".csv").toFile();
		try (OutputStream output = new FileOutputStream(this.dictionary)) {
			T12Classify.class.getClassLoader()
					.getResourceAsStream(String.format("classifier/%s/dictionary.csv", this.data))
					.transferTo(output);
		}
		this.categories = Utility.resource(String.format("classifier/%s/categories.csv", this.data))
				.lines()
				.collect(Collectors.toList());
		this.classifiers = new HashMap<>();
		for (String category : this.categories) {
			Classifier classifier = (Classifier) SerializationHelper.read(T12Classify.class.getClassLoader()
					.getResourceAsStream(String.format("classifier/%s/%s.classifier", this.data, category)));
			this.classifiers.put(category, classifier);
		}
		this.commentPattern = PATTERNS.get(this.data);
	}

	public SortedMap<Range, List<String>> label(String source) throws Exception {
		SortedMap<Range, List<String>> result = new TreeMap<>();
		Map<Range, String> comments = this.comments(source);
		for (Map.Entry<Range, String> comment : comments.entrySet()) {
			int offset = comment.getKey().start;
			for (Map.Entry<Range, List<String>> sentence : this.classify(comment.getValue()).entrySet()) {
				result.put(sentence.getKey().add(offset, offset), sentence.getValue());
			}
		}
		return result;
	}

	private SortedMap<Range, String> comments(String source) {
		SortedMap<Range, String> result = new TreeMap<>();
		Matcher matcher = this.commentPattern.matcher(source);
		while (matcher.find()) {
			result.put(new Range(matcher.start(), matcher.end()), matcher.group());
		}
		return result;
	}

	public SortedMap<Range, List<String>> classify(String original) throws Exception {
		NormalizedString normalized = new NormalizedString(original);
		InstancesBuilder instancesBuilder = this.instancesBuilder();
		List<Range> rangesInNormalized = this.rangesInNormalized(normalized);
		List<Range> rangesInOriginal = this.rangesInOriginal(normalized, rangesInNormalized);
		for (Range range : rangesInNormalized) {
			instancesBuilder.add(range.in(normalized.normalized), false);
		}
		Instances instances = instancesBuilder.build();
		// remove categories
		for (int i = instances.numAttributes() - 1; i >= 0; i = i - 1) {
			String name = instances.attribute(i).name();
			if (name.startsWith("category-")) {
				instances.deleteAttributeAt(i);
			}
		}
		// add class
		instances.insertAttributeAt(new Attribute("result", List.of("0", "1")), instances.numAttributes());
		instances.setClassIndex(instances.numAttributes() - 1);
		SortedMap<Range, List<String>> results = new TreeMap<>();
		for (Range range : rangesInOriginal) {
			results.put(range, new ArrayList<>());
		}
		for (Map.Entry<String, Classifier> entry : this.classifiers.entrySet()) {
			String category = entry.getKey();
			Classifier classifier = entry.getValue();
			for (int i = 0; i < instances.numInstances(); i = i + 1) {
				double result = classifier.classifyInstance(instances.instance(i));
				if (result > 0) {
					results.get(rangesInOriginal.get(i)).add(category);
				}
			}
		}
		return results;
	}

	public List<Range> rangesInNormalized(NormalizedString normalized) {
		List<Range> ranges = new ArrayList<>();
		int i = 0;
		for (Sentence sentence : Parser.getInstance().parse(normalized.normalized)) {
			i = normalized.normalized.indexOf(sentence.getText().charAt(0), i);
			ranges.add(new Range(i, i + sentence.getText().length()));
			i = i + sentence.getText().length();
		}
		return ranges;
	}

	public List<Range> rangesInOriginal(NormalizedString normalized, List<Range> rangesInNormalized) {
		return rangesInNormalized.stream().map(normalized::normalizedToOriginal).collect(Collectors.toList());
	}

	private InstancesBuilder instancesBuilder() {
		return new InstancesBuilder("dataset", this.categories, this.heuristics, this.dictionary);
	}

}
