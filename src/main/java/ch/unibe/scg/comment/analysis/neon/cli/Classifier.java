package ch.unibe.scg.comment.analysis.neon.cli;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Classifier {

	public static final String[] FILTER_OPTIONS = new String[]{
			"-P", "tfidf-", // attribute prefix
			"-L", // lowercase
			"-stemmer", "weka.core.stemmers.IteratedLovinsStemmer", // stemmer
			"-C", // word counts
			"-T", // TF
			"-I" // IDF
	};

	public Instances instances(String name, List<String> categories, IdentityHashMap<String, Set<String>> sentences) {
		ArrayList<Attribute> attributes = new ArrayList<>();
		attributes.add(new Attribute("text", (List<String>) null, null));
		for (String category : categories) {
			attributes.add(new Attribute(category, List.of("0", "1")));
		}
		Instances instances = new Instances(name, attributes, sentences.size());
		for (Map.Entry<String, Set<String>> sentence : sentences.entrySet()) {
			DenseInstance instance = new DenseInstance(categories.size());
			instance.setValue(instances.attribute("text"), sentence.getKey());
			for (String category : categories) {
				instance.setValue(instances.attribute(category), sentence.getValue().contains(category) ? 1 : 0);
			}
			instances.add(instance);
		}
		return instances;
	}

}
