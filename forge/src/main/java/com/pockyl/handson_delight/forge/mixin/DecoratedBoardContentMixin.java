package com.pockyl.handson_delight.forge.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.phantomwing.choppersdelight.block.entity.DecoratedCuttingBoardBlockEntity;
import com.phantomwing.choppersdelight.renderer.DecoratedCuttingBoardRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.pockyl.handson_delight.client.HandsOnDelightClient;

/**
 * Chopper's Delight's decorated board has no block model: its renderer draws the board, its banner pattern and what lies on it.
 * While the station's scene is drawn, the scene draws the food, so only that part is stopped here and the board stays in sight. A
 * tool stuck in stays drawn, as on Farmer's Delight's board.
 */
@Pseudo
@Mixin(value = DecoratedCuttingBoardRenderer.class, remap = false)
abstract class DecoratedBoardContentMixin {
    @Inject(method = "renderBoardContent", at = @At("HEAD"), cancellable = true)
    private void handson$drawnByScene(DecoratedCuttingBoardBlockEntity board, float partialTick, PoseStack poseStack,
            MultiBufferSource buffers, int light, int overlay, CallbackInfo callback) {
        if (!board.isItemCarvingBoard() && HandsOnDelightClient.isSceneDrawn(board.getBlockPos())) {
            callback.cancel();
        }
    }
}
