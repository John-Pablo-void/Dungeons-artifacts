package dev.johnpablo.dungeonsartifacts.mixin;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StructureTemplate.class)
abstract class ShrineFrameMixin {
    @Inject(method="createEntityIgnoreException",at=@At("HEAD"))
    private static void anchorFrame(ProblemReporter reporter,ServerLevelAccessor level,CompoundTag tag,CallbackInfoReturnable<Optional<Entity>> cir) {
        // Vanilla relocates Pos but leaves block_pos in template-local coordinates.
        // Translate only this mod's tagged frames, after the structure's rotation/translation.
        if(tag.getBooleanOr("dungeons_artifacts_frame",false)) tag.read("Pos",Vec3.CODEC).ifPresent(pos -> tag.store("block_pos",BlockPos.CODEC,BlockPos.containing(pos)));
    }
}
