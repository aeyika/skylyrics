package dev.skylyrics.core;

import com.google.gson.*;
import java.util.ArrayList;
import java.util.Locale;

/** Strict v1 envelope parser shared by both clients. Never accepts arbitrary class names. */
public final class Protocol {
    public static final int VERSION = 1;
    public static final int MAX_MESSAGE_BYTES = 2 * 1024 * 1024;
    private Protocol() {}
    public static JsonObject parse(String text) {
        if (text.length() > MAX_MESSAGE_BYTES || text.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_MESSAGE_BYTES)
            throw new IllegalArgumentException("Message too large");
        JsonObject o = JsonParser.parseString(text).getAsJsonObject();
        if (number(o, "v") != VERSION) throw new IllegalArgumentException("Unsupported protocol version");
        String type = string(o, "type", 32);
        if (!java.util.Set.of("hello", "track", "playback", "lyrics", "ping").contains(type)) throw new IllegalArgumentException("Unknown message type");
        double seq = number(o, "seq");
        if (seq < 0 || seq != Math.floor(seq) || seq > 9_007_199_254_740_991d) throw new IllegalArgumentException("Invalid sequence");
        return o;
    }
    public static String string(JsonObject o, String key, int limit) {
        JsonElement e = o.get(key);
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isString()) throw new IllegalArgumentException("Expected text: " + key);
        String value = e.getAsString();
        if (value.length() > limit || value.indexOf('\0') >= 0) throw new IllegalArgumentException("Invalid text: " + key);
        return value;
    }
    public static double number(JsonObject o, String key) {
        JsonElement e = o.get(key);
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException("Expected number: " + key);
        double n = e.getAsDouble(); if (!Double.isFinite(n)) throw new IllegalArgumentException("Non-finite number"); return n;
    }
    public static boolean bool(JsonObject o, String key) {
        JsonElement e = o.get(key);
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isBoolean()) throw new IllegalArgumentException("Expected boolean: " + key);
        return e.getAsBoolean();
    }
    public static Lyrics lyrics(JsonObject o) {
        var lines = new ArrayList<Lyrics.Line>();
        JsonArray source = o.getAsJsonArray("lines");
        if (source == null || source.size() > 5000) throw new IllegalArgumentException("Invalid lines");
        int count = 0;
        for (JsonElement raw : source) {
            JsonObject l = raw.getAsJsonObject(); var segments = new ArrayList<Lyrics.Segment>();
            JsonArray ss = l.getAsJsonArray("segments");
            if (ss == null || (count += ss.size()) > 100_000) throw new IllegalArgumentException("Too many segments");
            for (JsonElement sraw : ss) {
                var s = sraw.getAsJsonObject();
                segments.add(new Lyrics.Segment(string(s,"text",4096), number(s,"startMs"), number(s,"endMs"), bool(s,"partOfWord")));
            }
            lines.add(new Lyrics.Line(number(l,"startMs"), number(l,"endMs"),
                    Lyrics.Alignment.valueOf(string(l,"alignment",16).toUpperCase(Locale.ROOT)),
                    Lyrics.Role.valueOf(string(l,"role",16).toUpperCase(Locale.ROOT)), segments));
        }
        return new Lyrics(string(o,"trackId",256), Lyrics.Sync.valueOf(string(o,"sync",16).toUpperCase(Locale.ROOT)), lines);
    }
}
