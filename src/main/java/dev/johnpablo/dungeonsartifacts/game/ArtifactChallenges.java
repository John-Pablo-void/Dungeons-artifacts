package dev.johnpablo.dungeonsartifacts.game;

import java.util.*;
import dev.johnpablo.dungeonsartifacts.DungeonsArtifacts;
import net.minecraft.server.level.ServerPlayer;

/** Uses the server clock and only successful activations; never trusts client counters. */
public final class ArtifactChallenges {
    private static final Map<UUID,ArrayDeque<Long>> LIGHTNING=new HashMap<>();
    public static void award(ServerPlayer p,String path) {
        var advancement=p.level().getServer().getAdvancements().get(DungeonsArtifacts.id("artifacts/"+path));
        if(advancement!=null)p.getAdvancements().award(advancement,"completed");
    }
    public static void lightning(ServerPlayer p) {
        long now=p.level().getServer().overworld().getGameTime();
        var uses=LIGHTNING.computeIfAbsent(p.getUUID(),id -> new ArrayDeque<>());
        while(!uses.isEmpty()&&uses.peekFirst()<now-100)uses.removeFirst();
        uses.addLast(now);
        if(uses.size()>=10)award(p,"god_of_lightning");
        while(uses.size()>10)uses.removeFirst();
    }
    public static void release(UUID player){LIGHTNING.remove(player);}
    public static void clear(){LIGHTNING.clear();}
}
