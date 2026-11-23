package com.pockyl.freeform_delight.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.pockyl.freeform_delight.HandsOnConfig;
import com.pockyl.freeform_delight.station.CfbOven;

/**
 * The burners on top of Cooking for Blockheads' oven, where its station is on: cookware stands on them as on a stove. Its own use
 * stands only the tools of its kitchen there (knives, Pam's cookware); Farmer's Delight's skillet and cooking pot clicked on a free
 * burner now stand on it too, and an empty hand on a burner takes back what stands there. Everything else is
 * its own use (its tools, its menu, its door held open). Named per loader as in {@code CfbOvenToolsMixin}.
 */
@Pseudo
@Mixin(targets = "net.blay09.mods.cookingforblockheads.block.OvenBlock", remap = false)
abstract class CfbOvenTopMixin {
    @Inject(method = {"use", "m_6227_"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void handson$burner(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit,
            CallbackInfoReturnable<InteractionResult> result) {
        handson$use(state, level, pos, player, hand, hit, result);
    }

    @Inject(method = "method_9534", at = @At("HEAD"), cancellable = true, require = 0)
    private void handson$burnerFabric(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit,
            CallbackInfoReturnable<InteractionResult> result) {
        handson$use(state, level, pos, player, hand, hit, result);
    }

    private static void handson$use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit,
            CallbackInfoReturnable<InteractionResult> result) {
        if (!HandsOnConfig.oven() || hit.getDirection() != Direction.UP) {
            return;
        }
        CfbOven oven = CfbOven.of(level.getBlockEntity(pos));
        if (oven == null) {
            return;
        }
        int burner = CfbOven.burnerAt(state, pos, hit.getLocation());
        ItemStack held = player.getItemInHand(hand);
        ItemStack there = oven.tool(burner);
        if (held.isEmpty() && !there.isEmpty() && !player.isSecondaryUseActive()) {
            if (!level.isClientSide()) {
                player.setItemInHand(hand, there.copy());
                oven.setTool(burner, ItemStack.EMPTY);
            }
            result.setReturnValue(InteractionResult.sidedSuccess(level.isClientSide()));
        } else if (CfbOven.standsOnBurner(held) && there.isEmpty()) {
            if (!level.isClientSide()) {
                oven.setTool(burner, held.split(1));
            }
            result.setReturnValue(InteractionResult.sidedSuccess(level.isClientSide()));
        }
    }
}
