package dev.skylyrics.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.*;
import java.security.SecureRandom;
import java.util.HexFormat;

/** One schema and path across Minecraft versions. GPU caches never belong here. */
public record Preferences(int schema, boolean enabled, double offsetMs, double distance,
                          double width, double scale, double opacity, boolean occlusion,
                          boolean blur, boolean glow, int port, String bridgeToken, boolean skyboxAnchor) {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    public Preferences(int schema, boolean enabled, double offsetMs, double distance, double width, double scale,
                       double opacity, boolean occlusion, boolean blur, boolean glow, int port, String bridgeToken) {
        this(schema,enabled,offsetMs,distance,width,scale,opacity,occlusion,blur,glow,port,bridgeToken,false);
    }
    public Preferences {
        if (schema != 1) throw new IllegalArgumentException("Unsupported preference schema");
        range(offsetMs, -10_000, 10_000); range(distance, 2, 100); range(width, 120, 1600);
        range(scale, .25, 4); range(opacity, .05, 1);
        if (port < 1024 || port > 65535 || bridgeToken == null || !bridgeToken.matches("[0-9a-f]{64}"))
            throw new IllegalArgumentException("Invalid bridge configuration");
    }
    private static void range(double value, double min, double max) {
        if (!Double.isFinite(value) || value < min || value > max) throw new IllegalArgumentException("Preference outside range");
    }
    public static Preferences defaults() {
        byte[] bytes = new byte[32]; new SecureRandom().nextBytes(bytes);
        return new Preferences(1, true, 0, 12, 440, 1, 1, true, true, true, 19584, HexFormat.of().formatHex(bytes));
    }
    public static Path sharedPath() {
        return Path.of(System.getProperty("skylyrics.configDir", Path.of(System.getProperty("user.home"), ".skylyrics").toString()), "preferences.json");
    }
    public static Preferences load(Path path) throws IOException {
        if (!Files.exists(path)) { var p = defaults(); p.save(path); return p; }
        try { return java.util.Objects.requireNonNull(JSON.fromJson(Files.readString(path), Preferences.class)); }
        catch (RuntimeException e) { throw new IOException("Invalid preferences at " + path + ": " + e.getMessage(), e); }
    }
    public void save(Path path) throws IOException {
        Files.createDirectories(path.toAbsolutePath().getParent());
        Path temporary = Files.createTempFile(path.toAbsolutePath().getParent(), "preferences-", ".tmp");
        try {
            Files.writeString(temporary, JSON.toJson(this));
            try { Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }
    public Preferences withOffset(double value) { return new Preferences(schema, enabled, value, distance, width, scale, opacity, occlusion, blur, glow, port, bridgeToken,skyboxAnchor); }
    public Preferences withEnabled(boolean value) { return new Preferences(schema, value, offsetMs, distance, width, scale, opacity, occlusion, blur, glow, port, bridgeToken,skyboxAnchor); }
    public Preferences withEffects(boolean depth, boolean soft, boolean bloom) { return new Preferences(schema, enabled, offsetMs, distance, width, scale, opacity, depth, soft, bloom, port, bridgeToken,skyboxAnchor); }
    public Preferences withPlacement(double d, double w, double s, double alpha) { return new Preferences(schema, enabled, offsetMs, d, w, s, alpha, occlusion, blur, glow, port, bridgeToken,skyboxAnchor); }
    public Preferences withSkyboxAnchor(boolean value) { return new Preferences(schema,enabled,offsetMs,distance,width,scale,opacity,occlusion,blur,glow,port,bridgeToken,value); }
}
