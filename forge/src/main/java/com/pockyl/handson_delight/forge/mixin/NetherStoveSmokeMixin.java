package com.pockyl.handson_delight.forge.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import umpaz.nethersdelight.common.block.entity.AbstractStoveBlockEntity;

import com.pockyl.handson_delight.client.HandsOnDelightClient;

/**
 * Nether's Delight's blackstone stove puffs dark smoke over the food on it; where the scene shows the grill, the station's own steam
 * and smoke tell how each piece is doing, as on Farmer's Delight's stove ({@code StoveSmokeMixin}).
 */
@Pseudo
@Mixin(value = AbstractStoveBlockEntity.class, remap = false)
abstract class NetherStoveSmokeMixin {
    @Inject(method = "particleTick", at = @At("HEAD"), cancellable = true)
    private static void handson$quietWhileShown(Level level, BlockPos pos, BlockState state, AbstractStoveBlockEntity<?, ?> stove,
            CallbackInfo callback) {
        if (HandsOnDelightClient.isShown(pos)) {
            callback.cancel();
        }
    }
}
