package dev.johnpablo.dungeonsartifacts.client.mixin;
import dev.johnpablo.dungeonsartifacts.client.GhostRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.*;
@Mixin(LivingEntityRenderState.class)
abstract class LivingRenderStateMixin implements GhostRenderState {
    @Unique private boolean dungeonsArtifacts$ghost;
    public boolean dungeonsArtifacts$isGhost(){return dungeonsArtifacts$ghost;}
    public void dungeonsArtifacts$setGhost(boolean ghost){dungeonsArtifacts$ghost=ghost;}
}
