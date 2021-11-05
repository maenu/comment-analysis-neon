package ch.unibe.scg.comment.analysis.neon.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NormalizedString {

	public final String original;
	public final String normalized;
	List<Change> changes;

	public NormalizedString(String original) {
		this.original = original;
		this.changes = new ArrayList<>();
		this.normalized = this.normalize();
	}

	public Range normalizedToOriginal(Range normalized) {
		assert Range.of(this.normalized).includes(normalized);
		for (int i = this.changes.size() - 1; i >= 0; i = i - 1) {
			Change change = this.changes.get(i);
			normalized = change.revert(normalized);
		}
		return normalized;
	}

	protected String normalize() {
		String normalized = this.original.toLowerCase();
		normalized = this.normalizeLineEnds(normalized);
		normalized = this.reduceAlphabet(normalized);
		normalized = this.reduceFloats(normalized);
		normalized = this.reduceSpaces(normalized);
		normalized = this.trimStart(normalized);
		normalized = this.trimEnd(normalized);
		normalized = this.reduceLineStarts(normalized);
		normalized = this.reduceLineEnds(normalized);
		normalized = this.reduceNewLines(normalized);
		normalized = this.joinLines(normalized);
		normalized = this.normalizeSentenceEnds(normalized);
		normalized = this.normalizeCommas(normalized);
		normalized = this.splitJoinedSentences(normalized);
		normalized = this.normalizeLineStarts(normalized);
		normalized = this.normalizeEg(normalized);
		normalized = this.normalizeIe(normalized);
		normalized = this.normalizeEtc(normalized);
		return normalized;
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
			this.changes.add(new Change(new Range(result.end(1), result.start(2)), 0));
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
			this.changes.add(new Change(new Range(result.end(1), result.start(2)), 1));
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

	public static class Range implements Comparable<Range> {

		/**
		 * inclusive
		 */
		public final int start;
		/**
		 * exclusive
		 */
		public final int end;

		public Range(int start, int end) {
			assert start <= end;
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

		@Override
		public int compareTo(Range o) {
			return this.start - o.start;
		}

	}

	public static class Change {

		public final Range range;
		public final int length;

		public Change(Range range, int length) {
			this.range = range;
			this.length = length;
		}

		/**
		 * Adapt the argument to the range it would span before applying this change.
		 *
		 * @param range
		 * @return
		 */
		public Range revert(Range range) {
			int delta = this.delta();
			Range affectedRange = this.range.add(0, delta);
			if (affectedRange.includes(range)) {
				return this.range;
			}
			if (range.isBefore(affectedRange)) {
				return range;
			}
			if (range.isAfter(affectedRange)) {
				return range.add(-delta, -delta);
			}
			if (range.end > affectedRange.end) {
				// range is overlapping on the right
				return new Range(range.start, Math.max(range.start, range.end - delta));
			}
			// range is overlapping on the left
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
