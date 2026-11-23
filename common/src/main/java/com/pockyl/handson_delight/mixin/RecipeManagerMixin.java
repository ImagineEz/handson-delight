package com.pockyl.handson_delight.mixin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.pockyl.handson_delight.assembly.AssemblyRecipes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Food put together on the cutting board: its recipes are found before the manager reads them and kept for the board as well. They
 * stay in the manager, for automation; a player's crafting grid does not make them ({@code ShapelessRecipeMixin}).
 */
@Mixin(RecipeManager.class)
abstract class RecipeManagerMixin {
    @Unique
    private static final String APPLY =
            "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V";

    @Unique
    private final Map<ResourceLocation, JsonObject> handson$taken = new HashMap<>();
    /** Whether this is a server's manager, which loads the recipes itself: a client's only gets them from its server. */
    @Unique
    private boolean handson$loads;

    @Inject(method = APPLY, at = @At("HEAD"))
    private void handson$findAssembled(Map<ResourceLocation, JsonElement> recipes, ResourceManager resources, ProfilerFiller profiler,
            CallbackInfo callback) {
        handson$loads = true;
        handson$taken.clear();
        AssemblyRecipes.collect(recipes, handson$taken);
    }

    /**
     * A recipe of a type of its own (Farmer's Delight's dough from water) is not made in a player's grid either: the grid makes what
     * else its stacks match, as if the dish were not there.
     */
    @Inject(method = "getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/Container;"
            + "Lnet/minecraft/world/level/Level;)Ljava/util/Optional;", at = @At("RETURN"), cancellable = true)
    private void handson$notInThisGrid(RecipeType<?> type, Container grid, Level level, CallbackInfoReturnable<Optional<?>> callback) {
        if (callback.getReturnValue().orElse(null) instanceof Recipe<?> recipe && AssemblyRecipes.notInThisGrid(recipe, grid)) {
            callback.setReturnValue(handson$firstInThisGrid(type, grid, level));
        }
    }

    @Unique
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Optional<?> handson$firstInThisGrid(RecipeType<?> type, Container grid, Level level) {
        return AssemblyRecipes.firstInThisGrid((RecipeManager) (Object) this, (RecipeType) type, grid, level);
    }

    /**
     * Mods that list every match (Polymorph's choice among recipes) do not find it either; automation and other containers keep it.
     */
    @Inject(method = "getRecipesFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/Container;"
            + "Lnet/minecraft/world/level/Level;)Ljava/util/List;", at = @At("RETURN"), cancellable = true)
    private void handson$noneInThisGrid(RecipeType<?> type, Container grid, Level level, CallbackInfoReturnable<List<?>> callback) {
        List<?> found = callback.getReturnValue();
        if (found.stream().anyMatch(recipe -> recipe instanceof Recipe<?> made && AssemblyRecipes.notInThisGrid(made, grid))) {
            callback.setReturnValue(found.stream()
                    .filter(recipe -> !(recipe instanceof Recipe<?> made && AssemblyRecipes.notInThisGrid(made, grid)))
                    .collect(Collectors.toList()));
        }
    }

    /**
     * What a player's grid leaves comes from the recipe that made what was taken: the one the grid makes, which may be another recipe
     * of a dish's ingredients; failing that the dish itself. When another mod's crafting table made a dish by a lookup of its own,
     * vanilla would find no recipe and give the grid's own stacks back, doubling them and losing the last of each (Farmer's Delight's
     * dough from water: the wheat doubled and the water bucket was gone).
     */
    @Inject(method = "getRemainingItemsFor", at = @At("HEAD"), cancellable = true)
    private void handson$leftByTheRecipeItself(RecipeType<?> type, Container grid, Level level,
            CallbackInfoReturnable<NonNullList<ItemStack>> callback) {
        if (grid instanceof TransientCraftingContainer) {
            handson$recipeFor(type, grid, level)
                    .or(() -> AssemblyRecipes.seeingEveryRecipe(() -> handson$recipeFor(type, grid, level)))
                    .ifPresent(recipe -> callback.setReturnValue(recipe.getRemainingItems(grid)));
        }
    }

    @Unique
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Optional<Recipe<Container>> handson$recipeFor(RecipeType<?> type, Container grid, Level level) {
        return (Optional) ((RecipeManager) (Object) this).getRecipeFor((RecipeType) type, grid, level);
    }

    /** Another mod set the server's recipes anew after they loaded: Culinary Compat adds its cuts of several things this way. */
    @Inject(method = "replaceRecipes", at = @At("TAIL"))
    private void handson$readReplaced(Iterable<Recipe<?>> recipes, CallbackInfo callback) {
        if (handson$loads) {
            AssemblyRecipes.recipesReplaced(((RecipeManagerAccessor) this).handson$byName().values());
        }
    }

    @Inject(method = APPLY, at = @At("TAIL"))
    private void handson$keepAssembled(Map<ResourceLocation, JsonElement> recipes, ResourceManager resources, ProfilerFiller profiler,
            CallbackInfo callback) {
        AssemblyRecipes.load(handson$taken, (RecipeManager) (Object) this);
        handson$taken.clear();
    }
}
