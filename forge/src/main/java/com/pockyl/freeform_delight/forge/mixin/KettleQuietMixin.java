package com.pockyl.freeform_delight.forge.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import umpaz.farmersrespite.common.block.entity.KettleBlockEntity;

import com.pockyl.freeform_delight.HandsOnConfig;

/**
 * The kettle's own steam and whistle: the station's kettle steams from its spout and whistles when a brew is done, for every client
 * near it. Farmer's Respite's whistle is a flag its server sets for whichever kettle a client ticks next, heard in a single player
 * game only; its swirls at the spout are a potion's. With the station on, the station's take their place.
 */
@Pseudo
@Mixin(value = KettleBlockEntity.class, remap = false)
abstract class KettleQuietMixin {
    @Inject(method = "animationTick", at = @At("HEAD"), cancellable = true)
    private static void handson$stationSteams(Level level, BlockPos pos, BlockState state, KettleBlockEntity kettle, CallbackInfo callback) {
        if (HandsOnConfig.kettle()) {
            KettleBlockEntity.WHISTLE = false;
            callback.cancel();
        }
    }
}
