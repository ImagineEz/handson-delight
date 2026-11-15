package com.pockyl.handson_delight.forge.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.yirmiri.dungeonsdelight.common.block.DungeonStoveBlock;
import net.yirmiri.dungeonsdelight.common.block.entity.DungeonStoveBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Dungeons Delight 1.3.2 registers the dungeon stove's block entity, its ticker and where its six items lie, but its block makes
 * none ({@code newBlockEntity} returns null): food never stays on it, and the stove station has nothing to grill on. The block gets
 * the addon's own block entity, so it grills as Farmer's Delight's stove, with or without the station. A newer Dungeons Delight
 * that makes one itself is left alone.
 */
@Pseudo
@Mixin(DungeonStoveBlock.class)
abstract class DungeonStoveBlockMixin {
    @Inject(method = "newBlockEntity", at = @At("RETURN"), cancellable = true)
    private void handson$makeItsBlockEntity(BlockPos pos, BlockState state, CallbackInfoReturnable<BlockEntity> callback) {
        if (callback.getReturnValue() == null) {
            callback.setReturnValue(new DungeonStoveBlockEntity(pos, state));
        }
    }
}
