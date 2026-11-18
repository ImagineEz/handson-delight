package com.pockyl.handson_delight.station;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;

/**
 * Twilight's Flavor & Delight's maze stove cooks the food of the Twilight Forest and of its own mod in half the time (its
 * {@code placeFood} halves the recipe's time for those). Pieces laid at the station never pass that method, so the station halves
 * their time itself, and so does its book.
 */
public final class MazeStove {
    private static final ResourceLocation BLOCK = new ResourceLocation("twilightdelight", "maze_stove");
    private static final Set<String> FAST = Set.of("twilightforest", "twilightdelight");

    private MazeStove() {
    }

    /** The ticks {@code food} takes on {@code stove} for a recipe of {@code ticks}: half on the maze stove for Twilight food. */
    public static int ticks(BlockState stove, ItemStack food, int ticks) {
        if (!food.isEmpty() && BuiltInRegistries.BLOCK.getKey(stove.getBlock()).equals(BLOCK)
                && FAST.contains(BuiltInRegistries.ITEM.getKey(food.getItem()).getNamespace())) {
            return ticks / 2;
        }
        return ticks;
    }
}
