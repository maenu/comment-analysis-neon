package ch.unibe.scg.comment.analysis.neon.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NormalizedString {

	String original;
	List<Change> changes;
	String normalized;

	NormalizedString(String original) {
		this.original = original;
		this.changes = new ArrayList<>();
		this.normalized = null;
	}

	public static NormalizedString normalize(String original) {
		NormalizedString normalized = new NormalizedString(original);
		normalized.normalize();
		return normalized;
	}

	public Range normalizedToOriginal(Range normalized) {
		assert Range.of(this.normalized).includes(normalized);
		for (int i = this.changes.size() - 1; i >= 0; i = i - 1) {
			Change change = this.changes.get(i);
			normalized = change.revert(normalized);
		}
		return normalized;
	}

	public String getNormalized() {
		return this.normalized;
	}

	public void normalize() {
		this.normalized = this.original.toLowerCase();
		this.normalized = this.normalizeLineEnds(this.normalized);
		this.normalized = this.reduceAlphabet(this.normalized);
		this.normalized = this.reduceFloats(this.normalized);
		this.normalized = this.reduceSpaces(this.normalized);
		this.normalized = this.trimStart(this.normalized);
		this.normalized = this.trimEnd(this.normalized);
		this.normalized = this.reduceLineStarts(this.normalized);
		this.normalized = this.reduceLineEnds(this.normalized);
		this.normalized = this.reduceNewLines(this.normalized);
		this.normalized = this.joinLines(this.normalized);
		this.normalized = this.normalizeSentenceEnds(this.normalized);
		this.normalized = this.normalizeCommas(this.normalized);
		this.normalized = this.splitJoinedSentences(this.normalized);
		this.normalized = this.normalizeLineStarts(this.normalized);
		this.normalized = this.normalizeEg(this.normalized);
		this.normalized = this.normalizeIe(this.normalized);
		this.normalized = this.normalizeEtc(this.normalized);
	}

	protected String normalizeLineEnds(String original) {
		return this.change(original, "\r\n|\r", result -> {
			this.changes.add(new Change(new Range(result.start(), result.end()), 1));
			return "\n";
		});
	}

	protected String reduceAlphabet(String original) {
		return this.change(original, "[^a-z0-9,.!? \n]+", result -> {
			this.changes.add(new Change(new Range(result.start(), result.end()), 1));
			return " ";
		});
	}

	protected String reduceFloats(String original) {
		return this.change(original, "([0-9]+)\\.([0-9]+)", result -> {
			this.changes.add(new Change(new Range(result.end(1), result.end(1) + 1), 0));
			return "$1$2";
		});
	}

	protected String reduceSpaces(String original) {
		return this.change(original, " ( +)", result -> {
			this.changes.add(new Change(new Range(result.start(), result.end()), 1));
			return " ";
		});
	}

	protected String trimStart(String original) {
		return this.change(original, "^[ \n]+", result -> {
			this.changes.add(new Change(new Range(result.start(), result.end()), 0));
			return "";
		});
	}

	protected String trimEnd(String original) {
		return this.change(original, "[ \n]+$", result -> {
			this.changes.add(new Change(new Range(result.start(), result.end()), 0));
			return "";
		});
	}

	protected String reduceLineStarts(String original) {
		return this.change(original, "\n ", result -> {
			this.changes.add(new Change(new Range(result.start() + 1, result.end()), 0));
			return "\n";
		});
	}

	protected String reduceLineEnds(String original) {
		return this.change(original, " \n", result -> {
			this.changes.add(new Change(new Range(result.start(), result.end() - 1), 0));
			return "\n";
		});
	}

	protected String reduceNewLines(String original) {
		return this.change(original, "\n\n(\n+)", result -> {
			this.changes.add(new Change(new Range(result.start(1), result.end(1)), 0));
			return "\n\n";
		});
	}

	protected String joinLines(String original) {
		return this.change(original, "([^.!?\n])\n([^\n])", result -> {
			this.changes.add(new Change(new Range(result.end(1), result.end(1) + 1), 1));
			return "$1 $2";
		});
	}

	protected String normalizeSentenceEnds(String original) {
		return this.change(original, " ?([.!?])[ .!?]+", result -> {
			this.changes.add(new Change(new Range(result.start(), result.end()), 1));
			return "$1";
		});
	}

	protected String normalizeCommas(String original) {
		return this.change(original, " ?,,+", result -> {
			this.changes.add(new Change(new Range(result.start(), result.end()), 2));
			return ", ";
		});
	}

	protected String splitJoinedSentences(String original) {
		return this.change(original, "([,.!?])([^ ])", result -> {
			this.changes.add(new Change(new Range(result.end(1), result.end(1)), 1));
			return "$1 $2";
		});
	}

	protected String normalizeLineStarts(String original) {
		return this.change(original, "(\n|^)([ .!?]+)", result -> {
			this.changes.add(new Change(new Range(result.start(2), result.end(2)), 0));
			return "$1";
		});
	}

	protected String normalizeEg(String original) {
		return this.change(original, "(^|[^a-z0-9])e\\.? ?g\\.? ?($|[^.a-z0-9])", result -> {
			this.changes.add(new Change(new Range(result.end(1), result.start(2)), 2));
			return "$1eg$2";
		});
	}

	protected String normalizeIe(String original) {
		return this.change(original, "(^|[^a-z0-9])i\\.? ?e\\.? ?($|[^.a-z0-9])", result -> {
			this.changes.add(new Change(new Range(result.end(1), result.start(2)), 2));
			return "$1ie$2";
		});
	}

	protected String normalizeEtc(String original) {
		return this.change(original, "(^|[^a-z0-9])etc\\.? ?($|[^.a-z0-9])", result -> {
			this.changes.add(new Change(new Range(result.end(1), result.start(2)), 3));
			return "$1etc$2";
		});
	}

	protected String change(String original, String regex, Function<MatchResult, String> replacer) {
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(original);
		while (matcher.find()) {
			original = matcher.replaceFirst(replacer);
			matcher = pattern.matcher(original);
		}
		return original;
	}

	public static class Range {

		public final int start;
		public final int end;

		public Range(int start, int end) {
			this.start = start;
			this.end = end;
		}

		public static Range of(String string) {
			return new Range(0, string.length());
		}

		public boolean includes(Range range) {
			return this.start <= range.start && range.end <= this.end;
		}

		public boolean isBefore(Range range) {
			return this.end <= range.start;
		}

		public boolean isAfter(Range range) {
			return range.end <= this.start;
		}

		public int length() {
			return this.end - this.start;
		}

		public Range add(int s, int e) {
			return new Range(this.start + s, this.end + e);
		}

		public String in(String s) {
			return s.substring(this.start, this.end);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || this.getClass() != o.getClass()) {
				return false;
			}
			Range range = (Range) o;
			return this.start == range.start && this.end == range.end;
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.start, this.end);
		}

		@Override
		public String toString() {
			return "Range{" + "start=" + this.start + ", end=" + this.end + '}';
		}

	}

	public static class Change {

		public final Range range;
		public final int length;

		public Change(Range range, int length) {
			this.range = range;
			this.length = length;
		}

		public Range revert(Range range) {
			int delta = this.delta();
			Range whole = this.range.add(0, delta);
			if (whole.includes(range)) {
				return this.range;
			}
			if (range.isBefore(whole)) {
				return range;
			}
			if (range.isAfter(whole)) {
				return range.add(delta, delta);
			}
			if (range.end > whole.end) {
				// range is overlapping on the right
				return new Range(range.start, Math.max(range.start + 1, range.end - delta));
			}
			return new Range(Math.min(this.range.start, range.start), Math.max(this.range.end, range.end));
		}

		public int delta() {
			return this.length - this.range.length();
		}

		@Override
		public String toString() {
			return "Change{" + "range=" + this.range + ", length=" + this.length + '}';
		}

	}

}
