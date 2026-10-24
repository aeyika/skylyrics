package dev.skylyrics.core;

import java.util.List;
import java.util.Objects;

/** Version independent, immutable lyric document. All times are milliseconds. */
public record Lyrics(String trackId, Sync sync, List<Line> lines) {
    public enum Sync { STATIC, LINE, SYLLABLE }
    public enum Alignment { LEFT, RIGHT }
    public enum Role { LEAD, BACKGROUND, INTERLUDE }
    public Lyrics {
        Objects.requireNonNull(trackId); Objects.requireNonNull(sync);
        lines = List.copyOf(lines);
        if (trackId.length() > 256 || lines.size() > 5000) throw new IllegalArgumentException("Lyric document too large");
        double previous = -1;
        int segmentCount = 0;
        for (Line line : lines) {
            if (line.startMs() < previous) throw new IllegalArgumentException("Lines must be sorted by start time");
            previous = line.startMs(); segmentCount += line.segments().size();
        }
        if (segmentCount > 100_000) throw new IllegalArgumentException("Too many timed segments");
    }
    public record Segment(String text, double startMs, double endMs, boolean partOfWord) {
        public Segment {
            Objects.requireNonNull(text); checkTime(startMs, endMs);
            if (text.length() > 4096 || text.indexOf('\0') >= 0) throw new IllegalArgumentException("Invalid lyric text");
        }
    }
    public record Line(double startMs, double endMs, Alignment alignment, Role role, List<Segment> segments) {
        public Line {
            checkTime(startMs, endMs); Objects.requireNonNull(alignment); Objects.requireNonNull(role);
            segments = List.copyOf(segments);
            if (segments.isEmpty() || segments.size() > 4096) throw new IllegalArgumentException("Invalid segment count");
            double previous = startMs;
            for (Segment s : segments) {
                if (s.startMs() < previous || s.startMs() < startMs || s.endMs() > endMs)
                    throw new IllegalArgumentException("Segment lies outside its line or is unsorted");
                previous = s.startMs();
            }
        }
    }
    private static void checkTime(double start, double end) {
        if (!Double.isFinite(start) || !Double.isFinite(end) || start < 0 || end <= start || end > 86_400_000)
            throw new IllegalArgumentException("Invalid lyric time interval");
    }
}
