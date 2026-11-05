package dev.johnpablo.dungeonsartifacts.game;

import java.util.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.johnpablo.dungeonsartifacts.DungeonsArtifacts;
import dev.johnpablo.dungeonsartifacts.core.PlayerLedger;
import dev.johnpablo.dungeonsartifacts.soul.PlayerData;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/** World-saved echoes cannot burn, despawn, be stolen, or be lost when their chunk unloads. */
public final class SoulEchoes {
    public record Echo(UUID owner,GlobalPos location,int quarters) {
        public static final Codec<Echo> CODEC=RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("owner").forGetter(Echo::owner),GlobalPos.CODEC.fieldOf("location").forGetter(Echo::location),
            Codec.intRange(1,Integer.MAX_VALUE).fieldOf("quarters").forGetter(Echo::quarters)).apply(i,Echo::new));
    }
    public static final AttachmentType<List<Echo>> ECHOES=AttachmentRegistry.create(DungeonsArtifacts.id("soul_echoes"),b -> b.initializer(List::of).persistent(Echo.CODEC.listOf()));
    public static void onDeath(ServerPlayer p) {
        if(p.level().getGameRules().get(GameRules.KEEP_INVENTORY))return;
        // Equipment has already dropped at AFTER_DEATH: read the saved amount before capacity clamps.
        var old=p.getAttachedOrCreate(PlayerData.STATE);
        int amount=old.souls()*4+p.getAttachedOrCreate(PlayerData.FRACTION);
        if(amount>0) {
            BlockPos pos=p.blockPosition();
            if(pos.getY()<p.level().getMinY())pos=p.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,new BlockPos(pos.getX(),0,pos.getZ()));
            var world=p.level().getServer().overworld();var echoes=new ArrayList<>(world.getAttachedOrCreate(ECHOES));
            echoes.add(new Echo(p.getUUID(),GlobalPos.of(p.level().dimension(),pos),amount));world.setAttached(ECHOES,List.copyOf(echoes));
            p.sendSystemMessage(Component.translatable("message.dungeons_artifacts.echo_left",pos.getX(),pos.getY(),pos.getZ()));
        }
        p.setAttached(PlayerData.FRACTION,0);
        ArtifactServer.save(p,PlayerLedger.restore(ArtifactServer.capacity(p),new PlayerLedger.Snapshot(0,old.cooldowns(),-1)));
    }
    public static void tick(MinecraftServer server) {
        var world=server.overworld();var old=world.getAttachedOrCreate(ECHOES);
        if(old.isEmpty())return;
        var remaining=new ArrayList<Echo>();boolean changed=false;
        for(var echo:old) {
            var level=server.getLevel(echo.location.dimension());var p=server.getPlayerList().getPlayer(echo.owner);
            int quarters=echo.quarters;
            Vec3 center=Vec3.atBottomCenterOf(echo.location.pos()).add(0,.6,0);
            if(level!=null&&level.hasChunkAt(echo.location.pos())) {
                if(level.getGameTime()%5==0) {
                    double phase=level.getGameTime()*.08;
                    for(int k=0;k<6;k++) {double a=phase+k*Math.PI/3;level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,center.x+Math.cos(a)*.55,center.y+.2*Math.sin(phase),center.z+Math.sin(a)*.55,1,0,0,0,0);}
                    level.sendParticles(ParticleTypes.SCULK_SOUL,center.x,center.y+.4,center.z,1,.15,.25,.15,0);
                }
                if(p!=null&&p.isAlive()&&!p.isSpectator()&&p.level()==level&&p.position().distanceToSqr(center)<4) {
                    int added=SoulGathering.collectQuarters(p,quarters);quarters-=added;
                    if(added>0) {
                        changed=true;ArtifactServer.sound(p,SoundEvents.SOUL_ESCAPE.value(),1.4f);
                        p.sendOverlayMessage(Component.translatable("message.dungeons_artifacts.echo_reclaimed",added/4));
                        ArtifactChallenges.award(p,"soul_searching");
                    }
                }
            }
            if(quarters>0)remaining.add(new Echo(echo.owner,echo.location,quarters));
        }
        if(changed)world.setAttached(ECHOES,List.copyOf(remaining));
    }
}
