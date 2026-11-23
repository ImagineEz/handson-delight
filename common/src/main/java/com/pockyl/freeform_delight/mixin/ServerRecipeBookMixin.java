package com.pockyl.freeform_delight.mixin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.RecipeBook;
import net.minecraft.stats.ServerRecipeBook;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.pockyl.freeform_delight.HandsOnConfig;
import com.pockyl.freeform_delight.assembly.AssemblyRecipes;
import com.pockyl.freeform_delight.assembly.BookDishes;
import com.pockyl.freeform_delight.platform.Platform;

import java.util.Collection;
import java.util.List;

/**
 * The recipe book shows no sandwich: the crafting grid would not make it, it is put together on the cutting board. Farmer's
 * Delight's advancements still teach its recipes; they are left out here, and so are the ones a player learnt before, once their
 * client is known to have the mod ({@link BookDishes}). A player whose client lacks the mod crafts them in the grid and keeps them.
 */
@Mixin(ServerRecipeBook.class)
abstract class ServerRecipeBookMixin extends RecipeBook {
    @Shadow
    public abstract int addRecipes(Collection<Recipe<?>> recipes, ServerPlayer player);

    @Inject(method = "addRecipes", at = @At("HEAD"), cancellable = true)
    private void handson$notTheBoards(Collection<Recipe<?>> recipes, ServerPlayer player, CallbackInfoReturnable<Integer> callback) {
        if (!HandsOnConfig.removeCraftingRecipes() || !Platform.INSTANCE.hasBoard(player) || recipes.stream().noneMatch(recipe -> AssemblyRecipes.isMadeOnTheBoard(recipe.getId()))) {
            // A client without the mod crafts sandwiches in the grid, and its book shows them.
            return;
        }
        callback.setReturnValue(addRecipes(recipes.stream().filter(recipe -> !AssemblyRecipes.isMadeOnTheBoard(recipe.getId())).toList(), player));
    }

    /**
     * Not as the book loads: the player's client is not known yet, and one without the mod would lose the dishes for good (with
     * {@code doLimitedCrafting}, its grid then makes them no more). A Forge client is known as the book is first sent; a Fabric one
     * names its channels later and forgets them then.
     */
    @Inject(method = "sendInitialRecipeBook", at = @At("HEAD"))
    private void handson$forgetTheBoards(ServerPlayer player, CallbackInfo callback) {
        if (!HandsOnConfig.removeCraftingRecipes() || !Platform.INSTANCE.hasBoard(player)) {
            return;
        }
        // A plain loop: a method reference to this would name the mixin class itself, which never exists at run time.
        for (ResourceLocation id : List.copyOf(known)) {
            if (AssemblyRecipes.isMadeOnTheBoard(id)) {
                remove(id);
            }
        }
    }
}
