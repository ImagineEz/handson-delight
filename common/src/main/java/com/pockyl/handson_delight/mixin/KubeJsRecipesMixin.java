package com.pockyl.handson_delight.mixin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.pockyl.handson_delight.assembly.AssemblyRecipes;

import java.util.HashMap;
import java.util.Map;

/**
 * KubeJS for 1.20.1 loads the recipes itself as soon as a server script listens to its recipe event, and cancels the recipe manager's
 * own loading, so {@link RecipeManagerMixin} never sees its end: without this the board would make no dishes in such a modpack. The
 * board's recipes are found in the same JSON and read once KubeJS has put the recipes into the manager. Applied only with KubeJS
 * installed ({@code OptionalMixins}).
 * <p>
 * What was found is kept in a static field: ModernFix clears every instance map and collection of the event when {@code post}
 * returns, and in a modpack that often runs before this mixin's end, so an instance field would be read empty (GitHub #24). The
 * high priority applies this mixin after ModernFix's, so its end always runs second, as in such a modpack, and the game tests
 * with {@code -Pkubejs} (ModernFix included) see that order.
 */
@Pseudo
@Mixin(targets = "dev.latvian.mods.kubejs.recipe.RecipesEventJS", remap = false, priority = 2000)
abstract class KubeJsRecipesMixin {
    @Unique
    private static final Map<ResourceLocation, JsonObject> handson$taken = new HashMap<>();

    @Inject(method = "post", at = @At("HEAD"), require = 0)
    private void handson$findAssembled(RecipeManager manager, Map<ResourceLocation, JsonElement> recipes, CallbackInfo callback) {
        handson$taken.clear();
        AssemblyRecipes.collect(recipes, handson$taken);
    }

    @Inject(method = "post", at = @At("TAIL"), require = 0)
    private void handson$keepAssembled(RecipeManager manager, Map<ResourceLocation, JsonElement> recipes, CallbackInfo callback) {
        AssemblyRecipes.load(handson$taken, ((RecipeManagerAccessor) manager).handson$byName());
        handson$taken.clear();
    }
}
