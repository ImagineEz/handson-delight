package com.pockyl.handson_delight.assembly;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerRecipeBook;
import net.minecraft.world.item.crafting.Recipe;

import com.pockyl.handson_delight.HandsOnConfig;

import java.util.List;

/**
 * The board's dishes in a player's recipe book. A client with the mod makes them on the board, so its book leaves them out; a client
 * without it crafts them in the grid and keeps them, also in its save, for when it comes back with the mod or the config changes.
 */
public final class BookDishes {
    private BookDishes() {
    }

    /** Takes the board's dishes out of the book of a player whose client has the mod, and tells the client. */
    public static void forget(ServerPlayer player) {
        if (!HandsOnConfig.removeCraftingRecipes()) {
            return;
        }
        ServerRecipeBook book = player.getRecipeBook();
        List<Recipe<?>> dishes = player.server.getRecipeManager().getRecipes().stream()
                .filter(recipe -> AssemblyRecipes.isMadeOnTheBoard(recipe.getId()) && book.contains(recipe)).toList();
        if (!dishes.isEmpty()) {
            book.removeRecipes(dishes, player);
        }
    }
}
