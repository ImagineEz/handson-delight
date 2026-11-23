package com.pockyl.freeform_delight.station;

import com.pockyl.freeform_delight.HandsOnDelight;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.List;

/** The first freeform dish uses Farmer's Delight's existing bowl presentation. */
public final class FreeformMeal extends Item {
    public static final String ID = "freeform_meal";
    public static final String DATA = "FreeformCooking";

    public FreeformMeal() {
        super(new Properties().stacksTo(1).craftRemainder(Items.BOWL)
                .food(new FoodProperties.Builder().nutrition(6).saturationMod(0.6F).build()));
    }

    public static ItemStack display() {
        return new ItemStack(BuiltInRegistries.ITEM.get(HandsOnDelight.id(ID)));
    }

    static ItemStack cooked(List<ItemStack> ingredients, int ticks, int grade) {
        ItemStack dish = display();
        if (dish.isEmpty()) {
            return dish;
        }
        ListTag entries = new ListTag();
        for (ItemStack ingredient : ingredients) {
            if (!ingredient.isEmpty()) {
                CompoundTag entry = new CompoundTag();
                entry.putString("Item", BuiltInRegistries.ITEM.getKey(ingredient.getItem()).toString());
                entry.putInt("Count", ingredient.getCount());
                entries.add(entry);
            }
        }
        CompoundTag data = new CompoundTag();
        data.putInt("Version", 1);
        data.put("Ingredients", entries);
        data.putInt("StirTicks", ticks);
        data.putInt("StirGrade", grade);
        dish.getOrCreateTag().put(DATA, data);
        return dish;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity eater) {
        ItemStack left = super.finishUsingItem(stack, level, eater);
        if (eater instanceof Player player && player.getAbilities().instabuild) {
            return left;
        }
        return left.isEmpty() ? new ItemStack(Items.BOWL) : left;
    }
}
