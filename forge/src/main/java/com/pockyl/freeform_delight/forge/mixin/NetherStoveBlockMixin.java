package com.pockyl.freeform_delight.forge.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import umpaz.nethersdelight.common.block.AbstractStoveBlock;

import com.pockyl.freeform_delight.station.StoveCooking;

/**
 * Nether's Delight's blackstone stove used by a player whose client has no Hands-On Delight: the pieces a cook laid at the station go
 * its own way, as on Farmer's Delight's stove ({@code StoveBlockMixin}).
 */
@Pseudo
@Mixin(AbstractStoveBlock.class)
abstract class NetherStoveBlockMixin {
    @Inject(method = "use", at = @At("HEAD"))
    private void handson$handOverWithoutTheMod(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit, CallbackInfoReturnable<InteractionResult> callback) {
        StoveCooking.usedInTheWorld(player, level, pos);
    }
}
