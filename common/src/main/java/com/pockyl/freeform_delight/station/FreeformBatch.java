package com.pockyl.freeform_delight.station;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Keeps the inputs in their original slots until a freeform batch can safely be served. */
final class FreeformBatch {
    static final int STIR_TICKS = 60;
    final List<ItemStack> ingredients;
    int ticks;
    int grade;

    private FreeformBatch(List<ItemStack> ingredients, int ticks, int grade) {
        this.ingredients = ingredients;
        this.ticks = ticks;
        this.grade = grade;
    }

    static FreeformBatch start(KitchenPot pot) {
        List<ItemStack> ingredients = new ArrayList<>(pot.handson$slots());
        for (int slot = 0; slot < pot.handson$slots(); slot++) {
            ingredients.add(pot.handson$getItem(slot).copy());
        }
        return hasIngredients(ingredients) ? new FreeformBatch(ingredients, 0, 0) : null;
    }

    boolean matches(KitchenPot pot) {
        for (int slot = 0; slot < ingredients.size(); slot++) {
            if (!ItemStack.matches(ingredients.get(slot), pot.handson$getItem(slot))) {
                return false;
            }
        }
        return true;
    }

    CompoundTag save() {
        ListTag slots = new ListTag();
        for (ItemStack ingredient : ingredients) {
            slots.add(ingredient.save(new CompoundTag()));
        }
        CompoundTag data = new CompoundTag();
        data.put("Slots", slots);
        data.putInt("StirTicks", ticks);
        data.putInt("StirGrade", grade);
        return data;
    }

    static FreeformBatch load(CompoundTag data, int slotCount) {
        ListTag slots = data.getList("Slots", Tag.TAG_COMPOUND);
        if (slots.size() != slotCount) {
            return null;
        }
        List<ItemStack> ingredients = new ArrayList<>(slotCount);
        for (int slot = 0; slot < slotCount; slot++) {
            ingredients.add(ItemStack.of(slots.getCompound(slot)));
        }
        return hasIngredients(ingredients)
                ? new FreeformBatch(ingredients, Mth.clamp(data.getInt("StirTicks"), 0, STIR_TICKS),
                        Mth.clamp(data.getInt("StirGrade"), 0, (int) StirSkill.MOST)) : null;
    }

    private static boolean hasIngredients(List<ItemStack> ingredients) {
        return ingredients.stream().anyMatch(stack -> !stack.isEmpty());
    }
}
