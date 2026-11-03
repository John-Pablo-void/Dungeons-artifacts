package dev.johnpablo.dungeonsartifacts.core;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import static dev.johnpablo.dungeonsartifacts.core.Artifact.*;
import static dev.johnpablo.dungeonsartifacts.core.PlayerLedger.*;

/** Dependency-free regression suite: run scripts/test-core.sh with JDK 25. */
public final class PlayerLedgerTest {
    private static int assertions;
    private static void eq(Object expected, Object actual) {
        assertions++;
        if (!expected.equals(actual)) throw new AssertionError("Expected " + expected + ", got " + actual);
    }
    private static void throwsException(Runnable operation) {
        assertions++;
        try { operation.run(); }
        catch (IllegalArgumentException | ArithmeticException expected) { return; }
        throw new AssertionError("Expected invalid input to be rejected");
    }
    public static void main(String[] args) {
        PlayerLedger a = new PlayerLedger(50);
        PlayerLedger b = new PlayerLedger(50);
        AtomicInteger effects = new AtomicInteger();
        eq(Activation.INSUFFICIENT_SOULS, a.activate(SOUL_HEALER, 0, () -> { effects.incrementAndGet(); return true; }));
        eq(0, effects.get());
        eq(50, a.collect(Integer.MAX_VALUE));
        eq(0, a.collect(1));
        eq(Activation.NO_EFFECT, a.activate(SOUL_HEALER, 0, () -> false));
        eq(50, a.souls());
        eq(0L, a.remainingHalfTicks(SOUL_HEALER, 0));
        eq(Activation.SUCCESS, a.activate(SOUL_HEALER, 0, () -> true));
        eq(45, a.souls());
        eq(0, b.souls());
        eq(Activation.COOLDOWN, a.activate(SOUL_HEALER, 198, () -> true));
        eq(Activation.SUCCESS, a.activate(SOUL_HEALER, 200, () -> true));
        eq(40, a.souls());
        eq(Activation.SUCCESS, a.activate(LIGHTNING_ROD, 200, () -> true));
        eq(Activation.SUCCESS, a.activate(LIGHTNING_ROD, 200, () -> true));
        eq(36, a.souls());
        eq(0L, a.remainingHalfTicks(LIGHTNING_ROD, 200));
        a.setCapacity(10);
        eq(10, a.souls());
        eq(Activation.INSUFFICIENT_SOULS, a.activate(GHOST_CLOAK, 200, () -> true));
        var saved = a.snapshot();
        a.collect(50);
        var restored = PlayerLedger.restore(5, saved);
        eq(5, restored.souls());
        eq(200L, restored.remainingHalfTicks(SOUL_HEALER, 200));
        try { saved.cooldowns().clear(); throw new AssertionError("Mutable snapshot"); }
        catch (UnsupportedOperationException expected) { assertions++; }

        a = new PlayerLedger(50);
        eq(Boots.STARTED, a.toggleBoots(0));
        eq(998L, a.bootsRemainingHalfTicks(2));
        eq(Boots.STOPPED, a.toggleBoots(2));
        eq(1L, a.remainingHalfTicks(BOOTS_OF_LEVITATION, 2));
        eq(0.025, seconds(a.remainingHalfTicks(BOOTS_OF_LEVITATION, 2)));
        eq(Boots.COOLDOWN, a.toggleBoots(2));
        eq(Boots.STARTED, a.toggleBoots(4));
        eq(Boots.STOPPED, a.stopBoots(250));
        eq(123L, a.remainingHalfTicks(BOOTS_OF_LEVITATION, 250));
        var reconnected = PlayerLedger.restore(50, a.snapshot());
        eq(Boots.COOLDOWN, reconnected.toggleBoots(372));
        eq(Boots.STARTED, reconnected.toggleBoots(374));
        eq(Boots.INACTIVE, reconnected.tickBoots(1372));
        eq(Boots.EXPIRED, reconnected.tickBoots(1374));
        eq(600L, reconnected.remainingHalfTicks(BOOTS_OF_LEVITATION, 1374));
        eq(Boots.COOLDOWN, reconnected.toggleBoots(1972));
        eq(Boots.STARTED, reconnected.toggleBoots(1974));
        eq(Boots.EXPIRED, reconnected.toggleBoots(2974));
        eq(Boots.COOLDOWN, reconnected.toggleBoots(2974));

        for (Artifact artifact : Artifact.values()) {
            if (artifact == BOOTS_OF_LEVITATION) continue;
            PlayerLedger ledger = new PlayerLedger(50);
            ledger.collect(50);
            eq(Activation.SUCCESS, ledger.activate(artifact, 0, () -> true));
            eq(50 - artifact.soulCost, ledger.souls());
            eq(artifact.cooldownTicks * 2L, ledger.remainingHalfTicks(artifact, 0));
        }
        PlayerLedger nested = new PlayerLedger(50);
        nested.collect(50);
        eq(Activation.SUCCESS, nested.activate(SOUL_HEALER, 0, () -> {
            eq(Activation.BUSY, nested.activate(LIGHTNING_ROD, 0, () -> true));
            return true;
        }));
        throwsException(() -> nested.collect(-1));
        throwsException(() -> nested.setCapacity(-1));
        throwsException(() -> new Snapshot(-1, Map.of(), -1));
        throwsException(() -> new Snapshot(0, Map.of(SOUL_HEALER, -5L), -1));
        throwsException(() -> nested.toggleBoots(1));
        throwsException(() -> clock(Long.MAX_VALUE));
        throwsException(() -> nested.activate(BOOTS_OF_LEVITATION, 0, () -> true));
        System.out.println("PASS: " + assertions + " core assertions; no Minecraft gameplay tests executed.");
    }
}
