package dev.johnpablo.dungeonsartifacts.core;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * Per-player server state, independent of Minecraft serialization and networking.
 * Use the persistent overworld game time, never client time, as the clock.
 * One half-tick is 25 ms at 20 TPS. This keeps the boots' half-duration cooldown
 * exact even when the active duration has an odd number of ticks. Enforcement
 * occurs at the next server tick; display time is not rounded to whole seconds.
 * Call only from the owning server thread. Snapshot after every state change.
 */
public final class PlayerLedger {
    public enum Activation { SUCCESS, COOLDOWN, INSUFFICIENT_SOULS, NO_EFFECT, BUSY }
    public enum Boots { STARTED, STOPPED, EXPIRED, COOLDOWN, INACTIVE }
    public static final long MAX_BOOTS_HALF_TICKS = 1_000; // 25 seconds
    public static final long FULL_BOOTS_COOLDOWN_HALF_TICKS = 600; // 15 seconds

    public record Snapshot(int souls, Map<Artifact, Long> cooldowns, long bootsStarted) {
        public Snapshot {
            if (souls < 0 || bootsStarted < -1) throw new IllegalArgumentException("Invalid persisted state");
            cooldowns = Map.copyOf(cooldowns);
            if (cooldowns.values().stream().anyMatch(v -> v < 0)) {
                throw new IllegalArgumentException("Negative cooldown deadline");
            }
        }
    }

    private final EnumMap<Artifact, Long> cooldowns = new EnumMap<>(Artifact.class);
    private int souls;
    private int capacity;
    private long bootsStarted = -1;
    private boolean activating;

    public PlayerLedger(int capacity) { setCapacity(capacity); }

    public static PlayerLedger restore(int capacity, Snapshot snapshot) {
        PlayerLedger ledger = new PlayerLedger(capacity);
        ledger.souls = Math.min(capacity, snapshot.souls());
        ledger.cooldowns.putAll(snapshot.cooldowns());
        ledger.bootsStarted = snapshot.bootsStarted();
        return ledger;
    }

    public Snapshot snapshot() { return new Snapshot(souls, cooldowns, bootsStarted); }
    public int souls() { return souls; }
    public int capacity() { return capacity; }
    public boolean bootsActive() { return bootsStarted >= 0; }

    public void setCapacity(int capacity) {
        if (capacity < 0) throw new IllegalArgumentException("Negative capacity");
        this.capacity = capacity;
        souls = Math.min(souls, capacity);
    }

    /** Returns the number actually collected; safe for very large rewards. */
    public int collect(int reward) {
        if (reward < 0) throw new IllegalArgumentException("Negative reward");
        int added = (int) Math.min((long) capacity - souls, reward);
        souls += added;
        return added;
    }

    public long remainingHalfTicks(Artifact artifact, long now) {
        checkClock(now);
        return Math.max(0, cooldowns.getOrDefault(artifact, 0L) - now);
    }

    /**
     * Validate, run a server-side effect, then charge only for success.
     * The callback must return false without changing the world when no effect
     * is possible. It must not mutate this ledger. Reentrant activation is denied.
     */
    public Activation activate(Artifact artifact, long now, int cost, int cooldownTicks,
                               BooleanSupplier effect) {
        Objects.requireNonNull(artifact);
        Objects.requireNonNull(effect);
        checkClock(now);
        if (artifact == Artifact.BOOTS_OF_LEVITATION) {
            throw new IllegalArgumentException("Use toggleBoots for the boots state machine");
        }
        if (cost < 0 || cooldownTicks < 0) throw new IllegalArgumentException("Negative cost or cooldown");
        long deadline = Math.addExact(now, Math.multiplyExact((long) cooldownTicks, 2));
        if (activating) return Activation.BUSY;
        if (remainingHalfTicks(artifact, now) > 0) return Activation.COOLDOWN;
        if (souls < cost) return Activation.INSUFFICIENT_SOULS;
        activating = true;
        try {
            if (!effect.getAsBoolean()) return Activation.NO_EFFECT;
            souls -= cost;
            if (cooldownTicks > 0) cooldowns.put(artifact, deadline);
            return Activation.SUCCESS;
        } finally {
            activating = false;
        }
    }

    public Activation activate(Artifact artifact, long now, BooleanSupplier effect) {
        return activate(artifact, now, artifact.soulCost, artifact.cooldownTicks, effect);
    }

    public Boots toggleBoots(long now) {
        checkClock(now);
        if (bootsActive()) return stopBoots(now);
        if (remainingHalfTicks(Artifact.BOOTS_OF_LEVITATION, now) > 0) return Boots.COOLDOWN;
        bootsStarted = now;
        return Boots.STARTED;
    }

    /** Call each server tick. Expiration is also enforced by manual deactivation. */
    public Boots tickBoots(long now) {
        checkClock(now);
        if (bootsActive() && elapsedBoots(now) >= MAX_BOOTS_HALF_TICKS) return stopBoots(now);
        return Boots.INACTIVE;
    }

    /** Also used on disconnect, death and dimension change to prevent reset exploits. */
    public Boots stopBoots(long now) {
        checkClock(now);
        if (!bootsActive()) return Boots.INACTIVE;
        long elapsed = elapsedBoots(now);
        boolean expired = elapsed >= MAX_BOOTS_HALF_TICKS;
        // Callers supply even half-tick timestamps from server ticks; this gives
        // exact half-duration rather than truncating to whole seconds/ticks.
        long delay = expired ? FULL_BOOTS_COOLDOWN_HALF_TICKS : elapsed / 2;
        long deadline = Math.addExact(now, delay);
        bootsStarted = -1;
        cooldowns.put(Artifact.BOOTS_OF_LEVITATION, deadline);
        return expired ? Boots.EXPIRED : Boots.STOPPED;
    }

    public long bootsRemainingHalfTicks(long now) {
        checkClock(now);
        return bootsActive() ? Math.max(0, MAX_BOOTS_HALF_TICKS - elapsedBoots(now)) : 0;
    }

    private long elapsedBoots(long now) {
        if (now < bootsStarted) throw new IllegalArgumentException("Server clock moved backwards");
        return now - bootsStarted;
    }

    public static long clock(long serverTicks) {
        if (serverTicks < 0) throw new IllegalArgumentException("Negative server clock");
        return Math.multiplyExact(serverTicks, 2);
    }

    public static double seconds(long halfTicks) { return halfTicks / 40.0; }
    private static void checkClock(long now) {
        if (now < 0 || (now & 1) != 0) throw new IllegalArgumentException("Expected a whole server tick in half-tick units");
    }
}
