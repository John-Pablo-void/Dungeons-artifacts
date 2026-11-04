package dev.johnpablo.dungeonsartifacts;

import dev.johnpablo.dungeonsartifacts.core.Artifact;
import dev.johnpablo.dungeonsartifacts.effect.ArtifactEffects;
import dev.johnpablo.dungeonsartifacts.game.*;
import dev.johnpablo.dungeonsartifacts.item.ArtifactItems;
import dev.johnpablo.dungeonsartifacts.soul.PlayerData;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

public final class ArtifactClientTests implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        try(var world=context.worldBuilder().create()) {
            world.getServer().runOnServer(server -> {
                var p=world.getConnection().getServerPlayer();p.setGameMode(GameType.SURVIVAL);
                var ledger=ArtifactServer.ledger(p);ledger.collect(50);ArtifactServer.save(p,ledger);
                int slot=0;for(Artifact a:Artifact.values()) p.getInventory().setItem(slot++,new ItemStack(ArtifactItems.ITEMS.get(a)));
            });
            context.waitFor(mc -> mc.player!=null&&mc.player.getAttachedOrCreate(PlayerData.SOULS)==50);
            context.waitTicks(30);
            context.runOnClient(mc -> mc.gui.setScreen(new InventoryScreen(mc.player)));
            context.waitTicks(10);context.takeScreenshot("artifacts-inventory");
            context.setScreen(()->null);
            context.getInput().pressKey(org.lwjgl.glfw.GLFW.GLFW_KEY_J);
            context.waitFor(mc -> dev.johnpablo.dungeonsartifacts.client.ArtifactsClient.showSouls);
            context.waitTicks(10);context.takeScreenshot("artifacts-soul-hud");
            world.getServer().runOnServer(server -> {if(!GhostMovement.begin(world.getConnection().getServerPlayer()))throw new AssertionError("Ghost activation");});
            context.waitFor(mc -> mc.player.hasEffect(ArtifactEffects.GHOST));
            context.waitTicks(5);context.takeScreenshot("artifacts-ghost-hands");
            double[] origin=new double[3];
            world.getServer().runOnServer(server -> {
                var p=world.getConnection().getServerPlayer();origin[0]=p.getX();origin[1]=p.getY();origin[2]=p.getZ();
                p.teleportTo(p.level(),p.getX(),p.getY(),p.getZ(),java.util.Set.of(),0,0,false);
                for(int dx=-2;dx<=2;dx++)for(int dy=0;dy<3;dy++)
                    p.level().setBlock(p.blockPosition().offset(dx,dy,2),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
            });
            context.waitTicks(5);
            context.getInput().holdKey(options -> options.keyUp);
            context.waitTicks(40);
            context.getInput().releaseKey(options -> options.keyUp);
            world.getConnection().waitForServerboundPackets();
            world.getServer().runOnServer(server -> {
                var p=world.getConnection().getServerPlayer();
                if(p.getZ()<=origin[2]+3||Math.abs(p.getY()-origin[1])>.1) throw new AssertionError("Client/server wall phase failed: "+p.position());
            });
            world.getServer().runOnServer(server -> {
                var p=world.getConnection().getServerPlayer();
                var base=p.blockPosition();
                p.teleportTo(p.level(),base.getX()+.5,base.getY(),base.getZ()+.5,java.util.Set.of(),0,85,false);
                p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                var eye=net.minecraft.core.BlockPos.containing(p.getEyePosition());
                for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)for(int dy=0;dy<=1;dy++)p.level().setBlock(base.offset(dx,dy,dz),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
                p.level().setBlock(eye,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
                p.level().setBlock(p.blockPosition().below(),net.minecraft.world.level.block.Blocks.GOLD_BLOCK.defaultBlockState(),3);
                p.level().setBlock(eye.above(),net.minecraft.world.level.block.Blocks.DIAMOND_BLOCK.defaultBlockState(),3);
                p.teleportTo(p.level(),p.getX(),p.getY(),p.getZ(),java.util.Set.of(),0,85,false);
            });
            context.waitFor(mc -> mc.level.getBlockState(net.minecraft.core.BlockPos.containing(mc.player.getEyePosition())).is(net.minecraft.world.level.block.Blocks.STONE));
            context.waitTicks(5);context.takeScreenshot("ghost-adjacent-floor");
            world.getServer().runOnServer(server -> {var p=world.getConnection().getServerPlayer();p.teleportTo(p.level(),p.getX(),p.getY(),p.getZ(),java.util.Set.of(),0,-85,false);});
            context.waitTicks(5);context.takeScreenshot("ghost-adjacent-ceiling");
            world.getServer().runOnServer(server -> GhostMovement.end(world.getConnection().getServerPlayer(),true));
            context.waitFor(mc -> !mc.player.hasEffect(ArtifactEffects.GHOST));
        }
        // A normal world verifies placement through the real structure generator, not just template loading.
        try(var world=context.worldBuilder().setUseConsistentSettings(false).create()) {
            world.getServer().runOnServer(server -> {
                var level=server.overworld();
                var key=net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.STRUCTURE,DungeonsArtifacts.id("artifact_shrine"));
                var shrine=level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).getOrThrow(key);
                var player=world.getConnection().getServerPlayer();
                var found=level.getChunkSource().getGenerator().findNearestMapStructure(level,net.minecraft.core.HolderSet.direct(shrine),player.blockPosition(),64,false);
                if(found==null) throw new AssertionError("No naturally generated shrine found");
                var chunk=level.getChunk(found.getFirst());
                var start=chunk.getAllStarts().get(shrine.value());
                if(start==null||!start.isValid()) throw new AssertionError("Missing shrine structure start");
                var bounds=start.getBoundingBox();boolean potFound=false;
                for(var pos:net.minecraft.core.BlockPos.betweenClosed(bounds.minX(),bounds.minY(),bounds.minZ(),bounds.maxX(),bounds.maxY(),bounds.maxZ())) {
                    level.getChunk(pos);
                    if(level.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.DecoratedPotBlockEntity pot
                        && pot.getLootTable()!=null&&pot.getLootTable().identifier().equals(DungeonsArtifacts.id("pots/artifact_shrine"))) potFound=true;
                }
                if(!potFound) throw new AssertionError("Natural shrine has no loot pots");
                var frames=level.getEntitiesOfClass(net.minecraft.world.entity.decoration.ItemFrame.class,new net.minecraft.world.phys.AABB(bounds.minX(),bounds.minY(),bounds.minZ(),bounds.maxX()+1,bounds.maxY()+1,bounds.maxZ()+1));
                if(frames.stream().noneMatch(f -> !f.getItem().isEmpty()&&f.survives()))throw new AssertionError("Natural shrine lacks supported artifact frame");
                player.setGameMode(GameType.SPECTATOR);
                player.teleportTo(level,bounds.maxX()+6,bounds.maxY()+4,bounds.maxZ()+6,java.util.Set.of(),135,30,false);
            });
            context.waitTicks(40);context.takeScreenshot("artifacts-natural-shrine");
        }
    }
}
