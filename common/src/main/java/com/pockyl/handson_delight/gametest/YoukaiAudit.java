package com.pockyl.handson_delight.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;

import com.pockyl.handson_delight.station.CuisineBoardStation;
import com.pockyl.handson_delight.station.KegStation;
import com.pockyl.handson_delight.station.KettleStation;
import com.pockyl.handson_delight.station.KitchenCuisine;
import com.pockyl.handson_delight.station.KitchenKeg;
import com.pockyl.handson_delight.station.KitchenKettle;
import com.pockyl.handson_delight.station.KitchenYoukaiPot;
import com.pockyl.handson_delight.station.SteamerStation;
import com.pockyl.handson_delight.station.Youkai;
import com.pockyl.handson_delight.station.YoukaiPotDishes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The Youkai kitchen in the recipe audit, for Youkai's Feasts or Youkai's Homecoming, whichever is there: every recipe of its kettle,
 * cuisine board, pots, steamer and fermentation tank as the station reads it, and each recipe of the recipe manager that its station
 * does not read. Homecoming has recipes Feasts lacks (scarlet tea, the flesh roll, more sakes), read the same way.
 */
final class YoukaiAudit {
    private YoukaiAudit() {
    }

    /** Writes the kitchen's sections into {@code report} and returns the recipes a station does not read. */
    static List<String> write(GameTestHelper helper, List<String> report) {
        List<String> unread = new ArrayList<>();
        if (!Youkai.isLoaded()) {
            return unread;
        }
        ServerLevel level = helper.getLevel();
        KitchenKettle kettle = KitchenKettle.of(placed(helper, KettleStation.YOUKAI_KETTLE.get()));
        if (kettle != null) {
            section(report, unread, level, "kettle", "id | ingredients | base | result | ticks", kettle.handson$recipes(),
                    KitchenKettle.Brewing::id, brewing -> String.join(" | ", names(brewing.ingredients()),
                            fluid(brewing.base()) + " " + brewing.baseAmount(), fluid(brewing.result()), "" + brewing.ticks()));
        }
        KitchenCuisine board = KitchenCuisine.of(placed(helper, CuisineBoardStation.BOARD.get()));
        if (board != null) {
            section(report, unread, level, "cuisine", "id | result | steps (- by hand)", board.handson$dishes(), KitchenCuisine.Dish::id,
                    dish -> RecipeAudit.name(dish.result()) + " | " + dish.steps().stream()
                            .map(step -> step.isEmpty() ? "-" : RecipeAudit.name(step)).collect(Collectors.joining(", ")));
        }
        section(report, unread, level, "pot_cooking", "id | result | pot | ticks | ingredients", YoukaiPotDishes.of(level),
                KitchenYoukaiPot.Dish::id, dish -> String.join(" | ", RecipeAudit.name(dish.result()),
                        BuiltInRegistries.ITEM.getKey(dish.pot()).getPath(), "" + dish.ticks(), names(dish.ingredients())));
        section(report, unread, level, "steaming", "id | input | result | ticks", SteamerStation.recipes(level),
                SteamerStation.Steaming::id, steaming -> String.join(" | ", RecipeAudit.name(steaming.ingredient()),
                        RecipeAudit.name(steaming.result()), "" + steaming.ticks()));
        KitchenKeg tank = KitchenKeg.of(placed(helper, KegStation.TANK.get()));
        if (tank != null) {
            section(report, unread, level, "fermentation", "id | ingredients | base | result | ticks", tank.handson$recipes(),
                    KitchenKeg.Fermenting::id, fermenting -> String.join(" | ", names(fermenting.ingredients()),
                            fluid(fermenting.base()) + " " + fermenting.baseAmount(), fermenting.resultItem().isEmpty()
                            ? fluid(fermenting.resultFluid()) : RecipeAudit.name(fermenting.resultItem()), "" + fermenting.ticks()));
        }
        return unread;
    }

    private static BlockEntity placed(GameTestHelper helper, Block block) {
        BlockPos at = new BlockPos(1, 2, 1);
        helper.setBlock(at, block);
        return helper.getLevel().getBlockEntity(helper.absolutePos(at));
    }

    private static <T> void section(List<String> report, List<String> unread, ServerLevel level, String type, String columns,
            List<T> read, Function<T, ResourceLocation> id, Function<T, String> line) {
        Set<ResourceLocation> ids = read.stream().map(id).collect(Collectors.toSet());
        Set<String> missing = new TreeSet<>();
        for (Recipe<?> recipe : level.getRecipeManager().getRecipes()) {
            ResourceLocation recipeType = BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType());
            if (Youkai.id(type).equals(recipeType) && !ids.contains(recipe.getId())) {
                missing.add(recipe.getId().toString());
            }
        }
        report.add("");
        report.add("== YOUKAI " + type.toUpperCase() + " (" + read.size() + " read, " + Youkai.MOD_ID + ") ==");
        report.add(columns);
        read.stream().sorted(Comparator.comparing(entry -> id.apply(entry).toString()))
                .forEach(entry -> report.add(id.apply(entry) + " | " + line.apply(entry)));
        missing.forEach(recipe -> report.add(recipe + " | NOT READ"));
        missing.forEach(recipe -> unread.add(type + ": " + recipe));
    }

    private static String names(Collection<Ingredient> ingredients) {
        return ingredients.stream().map(RecipeAudit::name).collect(Collectors.joining(", "));
    }

    private static String fluid(Fluid fluid) {
        return BuiltInRegistries.FLUID.getKey(fluid).toString();
    }
}
