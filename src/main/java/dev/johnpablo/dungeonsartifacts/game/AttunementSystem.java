package dev.johnpablo.dungeonsartifacts.game;

import java.util.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.johnpablo.dungeonsartifacts.DungeonsArtifacts;
import dev.johnpablo.dungeonsartifacts.item.SoulItems;
import dev.johnpablo.dungeonsartifacts.soul.PlayerData;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/** Eight seed-derived sites anchored to the initial world spawn, saved across /setworldspawn. */
public final class AttunementSystem {
    public record Site(BlockPos pos,boolean built,boolean used) {
        static final Codec<Site> CODEC=RecordCodecBuilder.create(i -> i.group(
            BlockPos.CODEC.fieldOf("pos").forGetter(Site::pos),Codec.BOOL.fieldOf("built").forGetter(Site::built),
            Codec.BOOL.fieldOf("used").forGetter(Site::used)).apply(i,Site::new));
    }
    public static final AttachmentType<List<Site>> SITES=AttachmentRegistry.create(DungeonsArtifacts.id("altars"),b -> b.initializer(List::of).persistent(Site.CODEC.listOf()));
    private record Ritual(UUID player,int site,int ticks) {}
    private static final Map<UUID,Ritual> RITUALS=new HashMap<>();
    public static List<Site> layout(long seed,BlockPos spawn) {
        Random random=new Random(seed^0x534f554c414c5441L);List<Site> sites=new ArrayList<>();
        for(int i=0;i<8;i++) {
            double angle=(i+random.nextDouble()*.5)*Math.PI/4, radius=2100+random.nextDouble()*17700;
            int x=(Math.floorDiv(spawn.getX()+(int)(Math.cos(angle)*radius),16)*16)+8;
            int z=(Math.floorDiv(spawn.getZ()+(int)(Math.sin(angle)*radius),16)*16)+8;
            sites.add(new Site(new BlockPos(x,64,z),false,false));
        }
        return List.copyOf(sites);
    }
    public static List<Site> sites(ServerLevel level) {
        var sites=level.getAttachedOrCreate(SITES);
        if(sites.isEmpty()) {sites=layout(level.getSeed(),level.getRespawnData().globalPos().pos());level.setAttached(SITES,sites);}
        return sites;
    }
    private static void update(ServerLevel level,int index,Site site) {
        var sites=new ArrayList<>(sites(level));sites.set(index,site);level.setAttached(SITES,List.copyOf(sites));
    }
    public static void tick(ServerLevel level) {
        var sites=sites(level);
        for(int i=0;i<sites.size();i++) {
            Site site=sites.get(i);
            if(!site.built && level.hasChunkAt(site.pos)) {
                int y=level.getHeight(Heightmap.Types.WORLD_SURFACE,site.pos.getX(),site.pos.getZ());
                BlockPos center=new BlockPos(site.pos.getX(),Math.min(y,level.getMaxY()-8),site.pos.getZ());
                build(level,center);update(level,i,new Site(center,true,site.used));
            }
        }
    }
    public static void build(ServerLevel level,BlockPos center) {
        // Entire 11x11 structure lies in its target chunk; never loads surrounding chunks.
        for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++) {
            for(int y=1;y<=6;y++)level.setBlock(center.offset(x,y,z),Blocks.AIR.defaultBlockState(),3);
            level.setBlock(center.offset(x,0,z),(Math.abs(x)==5||Math.abs(z)==5?Blocks.CHISELED_DEEPSLATE:Blocks.DEEPSLATE_TILES).defaultBlockState(),3);
        }
        level.setBlock(center,Blocks.LODESTONE.defaultBlockState(),3);
        for(int x:new int[]{-4,4})for(int z:new int[]{-4,4}) {
            for(int y=1;y<=4;y++)level.setBlock(center.offset(x,y,z),Blocks.DEEPSLATE_BRICKS.defaultBlockState(),3);
            level.setBlock(center.offset(x,5,z),Blocks.SOUL_LANTERN.defaultBlockState(),3);
        }
        for(int v=-3;v<=3;v++)for(BlockPos pos:List.of(center.offset(v,5,-4),center.offset(v,5,4),center.offset(-4,5,v),center.offset(4,5,v)))
            level.setBlock(pos,Blocks.DEEPSLATE_BRICK_SLAB.defaultBlockState(),3);
    }
    public static void tickPlayer(ServerPlayer p) {
        ServerLevel world=p.level().getServer().overworld();var sites=sites(world);
        // Re-evaluate the nearest unused site, including after another player completes one.
        int nearest=-1;double distance=Double.MAX_VALUE;
        for(int i=0;i<sites.size();i++)if(!sites.get(i).used) {
            var pos=sites.get(i).pos;double d=Math.pow(pos.getX()-p.getX(),2)+Math.pow(pos.getZ()-p.getZ(),2);
            if(d<distance){distance=d;nearest=i;}
        }
        for(int slot=0;slot<p.getInventory().getContainerSize();slot++) {
            var stack=p.getInventory().getItem(slot);if(!stack.is(SoulItems.COMPASS))continue;
            if(nearest<0) {stack.remove(DataComponents.LODESTONE_TRACKER);continue;}
            var tracker=new LodestoneTracker(Optional.of(GlobalPos.of(Level.OVERWORLD,sites.get(nearest).pos)),false);
            if(!tracker.equals(stack.get(DataComponents.LODESTONE_TRACKER)))stack.set(DataComponents.LODESTONE_TRACKER,tracker);
            if(p.level()==world&&distance<400) {
                stack.shrink(1);p.sendSystemMessage(Component.translatable("message.dungeons_artifacts.compass_broke"));
                ArtifactServer.particles(world,ParticleTypes.SOUL,p.position().add(0,1,0),24);
            }
        }
        Ritual ritual=RITUALS.get(p.getUUID());
        if(ritual!=null) {
            Site site=sites.get(ritual.site);
            if(p.level()!=world||!p.isAlive()||p.isSpectator()||site.used||p.position().distanceToSqr(Vec3.atBottomCenterOf(site.pos.above()))>16) {RITUALS.remove(p.getUUID());return;}
            Vec3 center=Vec3.atBottomCenterOf(site.pos.above());
            ArtifactServer.effect(p,dev.johnpablo.dungeonsartifacts.effect.ArtifactEffects.ROOTED,5,0);
            p.teleportTo(center.x,center.y,center.z);p.setDeltaMovement(Vec3.ZERO);
            int tick=ritual.ticks+1;
            double radius=tick<40?1.6:Math.max(0,1.6*(80-tick)/40.0), height=Math.min(1.1,tick/36.0);
            for(int k=0;k<16;k++) {double a=k*Math.PI/8+tick*.08;world.sendParticles(ParticleTypes.SOUL,center.x+Math.cos(a)*radius,center.y+height,center.z+Math.sin(a)*radius,1,0,0,0,0);}
            if(tick>=80) {
                update(world,ritual.site,new Site(site.pos,true,true));
                int level=p.getAttachedOrCreate(PlayerData.ATTUNEMENT)+1;p.setAttached(PlayerData.ATTUNEMENT,level);
                world.setBlock(site.pos,Blocks.CRYING_OBSIDIAN.defaultBlockState(),3);
                p.sendSystemMessage(Component.translatable("message.dungeons_artifacts.attuned",level,level));
                RITUALS.remove(p.getUUID());
            }else RITUALS.put(p.getUUID(),new Ritual(p.getUUID(),ritual.site,tick));
            return;
        }
        if(p.level()!=world||!p.isAlive()||p.isSpectator()||p.isPassenger())return;
        for(int i=0;i<sites.size();i++) {
            Site site=sites.get(i);final int index=i;
            if(site.built&&!site.used&&p.blockPosition().equals(site.pos.above())&&p.onGround()&&RITUALS.values().stream().noneMatch(r -> r.site==index)) {
                RITUALS.put(p.getUUID(),new Ritual(p.getUUID(),i,0));break;
            }
        }
    }
    public static void release(UUID player){RITUALS.remove(player);}
    public static void clear(){RITUALS.clear();}
}
