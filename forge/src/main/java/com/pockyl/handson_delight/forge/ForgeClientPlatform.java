package com.pockyl.handson_delight.forge;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;

import com.pockyl.handson_delight.client.ClientPlatform;

/** Forge's baked models choose their GUI model themselves (separate transforms) while they apply the transform. */
public final class ForgeClientPlatform implements ClientPlatform {
    @Override
    public BakedModel guiModel(BakedModel model, PoseStack poseStack) {
        return model.applyTransform(ItemDisplayContext.GUI, poseStack, false);
    }

    @Override
    public BakedModel model(ResourceLocation id) {
        ModelManager models = Minecraft.getInstance().getModelManager();
        BakedModel model = models.getModel(id);
        return model == models.getMissingModel() ? null : model;
    }
}
