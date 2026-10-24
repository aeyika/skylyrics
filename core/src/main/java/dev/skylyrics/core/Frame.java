package dev.skylyrics.core;

import java.util.List;

/** Immutable extraction result. Drawing consumes only this snapshot and prepared GPU data. */
public record Frame(double timestampMs, String trackId, List<Line> lines, Panel panel) {
    public Frame { lines = List.copyOf(lines); }
    public record Panel(double distance, double width, double scale, double opacity, boolean occlusion, boolean blur, boolean glow, double fontSize, boolean skyboxAnchor) {
        public Panel(double distance,double width,double scale,double opacity,boolean occlusion,boolean blur,boolean glow,double fontSize){this(distance,width,scale,opacity,occlusion,blur,glow,fontSize,false);}
    }
    public record Line(int index, double y, double height, double opacity, double blur,
                       Lyrics.Alignment alignment, Lyrics.Role role, List<Segment> segments) {
        public Line { segments = List.copyOf(segments); }
    }
    public record Segment(String text, double x, double y, double width, double highlight,
                          double scale, double displacement, double opacity, double glow,
                          double glowRadius, double startMs, double endMs,
                          double gradientOffset, double gradientExtent, boolean verticalGradient) {}
}
