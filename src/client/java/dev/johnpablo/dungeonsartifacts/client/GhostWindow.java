package dev.johnpablo.dungeonsartifacts.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** A closed cutaway of the real floor, ceiling and perimeter at feet/head height. */
public final class GhostWindow {
    public static final int RADIUS=5;
    public static boolean contains(int x,int z){return Math.abs(x)+Math.abs(z)<=RADIUS;}
    public static void submit(Minecraft mc,float partial,SubmitNodeCollector collector) {
        var p=mc.player;BlockPos eyeBlock=BlockPos.containing(p.getEyePosition());
        var inside=p.level().getBlockState(eyeBlock);
        if(!inside.isViewBlocking(p.level(),eyeBlock))return;
        var base=p.blockPosition();Vec3 eye=p.getEyePosition(partial);
        PoseStack pose=new PoseStack();
        pose.mulPose(Axis.XP.rotationDegrees(p.getXRot(partial)));
        pose.mulPose(Axis.YP.rotationDegrees(p.getYRot(partial)+180));
        for(int dx=-RADIUS;dx<=RADIUS;dx++)for(int dz=-RADIUS;dz<=RADIUS;dz++) {
            if(!contains(dx,dz))continue;
            BlockPos cell=base.offset(dx,0,dz);
            face(mc,collector,pose,cell.below(),Direction.UP,inside,eye);
            face(mc,collector,pose,cell.above(2),Direction.DOWN,inside,eye);
            for(Direction d:Direction.Plane.HORIZONTAL) {
                if(contains(dx+d.getStepX(),dz+d.getStepZ()))continue;
                for(int y=0;y<2;y++)face(mc,collector,pose,cell.above(y).relative(d),d.getOpposite(),inside,eye);
            }
        }
    }
    private static void face(Minecraft mc,SubmitNodeCollector collector,PoseStack pose,BlockPos block,Direction direction,BlockState fallback,Vec3 eye) {
        var state=mc.level.getBlockState(block);
        // Seal air/transparent perimeter cells as well; distant caves must never leak through.
        if(!state.isViewBlocking(mc.level,block))state=fallback;
        var sprite=mc.getModelManager().getBlockStateModelSet().getParticleMaterial(state).sprite();
        float x=(float)(block.getX()-eye.x),y=(float)(block.getY()-eye.y),z=(float)(block.getZ()-eye.z);
        float[][] vertices=switch(direction) {
            case UP -> new float[][]{{x,y+1,z},{x,y+1,z+1},{x+1,y+1,z+1},{x+1,y+1,z}};
            case DOWN -> new float[][]{{x,y,z+1},{x,y,z},{x+1,y,z},{x+1,y,z+1}};
            case NORTH -> new float[][]{{x+1,y,z},{x,y,z},{x,y+1,z},{x+1,y+1,z}};
            case SOUTH -> new float[][]{{x,y,z+1},{x+1,y,z+1},{x+1,y+1,z+1},{x,y+1,z+1}};
            case WEST -> new float[][]{{x,y,z},{x,y,z+1},{x,y+1,z+1},{x,y+1,z}};
            case EAST -> new float[][]{{x+1,y,z+1},{x+1,y,z},{x+1,y+1,z},{x+1,y+1,z+1}};
        };
        int shade=direction==Direction.DOWN?0xff808080:direction==Direction.UP?0xffb0b0b0:0xff969696;
        collector.submitCustomGeometry(pose,RenderTypes.blockScreenEffect(sprite.atlasLocation()),(transform,builder) -> {
            float[] us={sprite.getU0(),sprite.getU1(),sprite.getU1(),sprite.getU0()};
            float[] vs={sprite.getV1(),sprite.getV1(),sprite.getV0(),sprite.getV0()};
            for(int i=0;i<4;i++)builder.addVertex(transform.pose(),vertices[i][0],vertices[i][1],vertices[i][2]).setUv(us[i],vs[i]).setColor(shade);
        });
    }
}
