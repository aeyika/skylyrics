package dev.skylyrics.core;

import java.util.function.LongSupplier;

/** Wall time is used only at the bridge boundary; playback advances on a monotonic clock. */
public final class PlaybackClock {
    private final LongSupplier nanos;
    private double positionMs, durationMs;
    private long anchor;
    private boolean playing;
    private boolean initialized;
    private double sourceHighWater;
    private static final double SEEK_THRESHOLD_MS=1000;
    public PlaybackClock() { this(System::nanoTime); }
    public PlaybackClock(LongSupplier nanos) { this.nanos = nanos; anchor = nanos.getAsLong(); }
    public synchronized void update(double position, double duration, boolean running, double messageAgeMs) {
        update(position,duration,running,messageAgeMs,false);
    }
    public synchronized void update(double position, double duration, boolean running, double messageAgeMs,boolean discontinuity) {
        if (!Double.isFinite(position) || !Double.isFinite(duration) || !Double.isFinite(messageAgeMs)
                || position < 0 || duration < 0 || position > duration || duration > 86_400_000)
            throw new IllegalArgumentException("Invalid playback position");
        long now=nanos.getAsLong();
        double supplied=Math.min(duration,position+(running?Math.max(0,Math.min(2000,messageAgeMs)):0));
        double elapsed=playing?Math.max(0,(now-anchor)/1_000_000d):0;
        double predicted=Math.min(duration,positionMs+elapsed),error=supplied-predicted;
        boolean seek=position<sourceHighWater-SEEK_THRESHOLD_MS
                ||(error>SEEK_THRESHOLD_MS&&position>sourceHighWater+SEEK_THRESHOLD_MS);
        if(!initialized||discontinuity||seek||playing!=running||durationMs!=duration||!running){
            positionMs=supplied;sourceHighWater=position;
        }else{
            // A stalled/coarse source must not pull the monotonic clock backward on every poll.
            // Fresh small corrections are slewed at <=10% of elapsed time, never a visible reset.
            double correction=position>sourceHighWater+1?Math.max(-elapsed*.1,Math.min(elapsed*.1,error*.1)):0;
            positionMs=Math.max(0,Math.min(duration,predicted+correction));
            sourceHighWater=Math.max(sourceHighWater,position);
        }
        initialized=true;durationMs=duration;playing=running;anchor=now;
    }
    public synchronized State snapshot(double offsetMs) {
        double elapsed = playing ? Math.max(0, (nanos.getAsLong() - anchor) / 1_000_000d) : 0;
        return new State(Math.max(0, Math.min(durationMs, positionMs + elapsed + offsetMs)), durationMs, playing);
    }
    public synchronized void pause() {
        positionMs = snapshot(0).positionMs(); playing = false; anchor = nanos.getAsLong();
    }
    public record State(double positionMs, double durationMs, boolean playing) {}
}
