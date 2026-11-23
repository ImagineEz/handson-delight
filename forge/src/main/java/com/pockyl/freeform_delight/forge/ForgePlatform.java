package com.pockyl.freeform_delight.forge;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.RecipeWrapper;
import vectorwing.farmersdelight.common.block.entity.CookingPotBlockEntity;
import vectorwing.farmersdelight.common.block.entity.CuttingBoardBlockEntity;
import vectorwing.farmersdelight.common.block.entity.AbstractStoveBlockEntity;
import vectorwing.farmersdelight.common.block.entity.SkilletBlockEntity;
import vectorwing.farmersdelight.common.crafting.CookingPotRecipe;
import vectorwing.farmersdelight.common.registry.ModRecipeTypes;

import com.pockyl.freeform_delight.FarmersDelightContent;
import com.pockyl.freeform_delight.platform.Platform;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Farmer's Delight on Forge: the pot keeps its items in Forge's item handler. */
public final class ForgePlatform implements Platform {
    private static final String CULINARY_COMPAT = "culinarycompat";

    @Override
    public ItemStack getItem(CookingPotBlockEntity pot, int slot) {
        return pot.getInventory().getStackInSlot(slot);
    }

    @Override
    public void setItem(CookingPotBlockEntity pot, int slot, ItemStack stack) {
        pot.getInventory().setStackInSlot(slot, stack);
    }

    @Override
    public Optional<CookingPotRecipe> findRecipe(Level level, List<ItemStack> items) {
        ItemStackHandler slots = new ItemStackHandler(CookingPotBlockEntity.INVENTORY_SIZE);
        for (int i = 0; i < Math.min(items.size(), CookingPotBlockEntity.MEAL_DISPLAY_SLOT); i++) {
            slots.setStackInSlot(i, items.get(i).copy());
        }
        return level.getRecipeManager().getRecipeFor(ModRecipeTypes.COOKING.get(), new RecipeWrapper(slots), level);
    }

    @Override
    public List<CookingPotRecipe> cookingRecipes(Level level) {
        return level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.COOKING.get());
    }

    @Override
    public ItemStack getItem(CuttingBoardBlockEntity board) {
        return board.getInventory().getStackInSlot(0);
    }

    @Override
    public void setItem(CuttingBoardBlockEntity board, ItemStack stack) {
        ((IItemHandlerModifiable) board.getInventory()).setStackInSlot(0, stack);
    }

    @Override
    public List<ItemStack> takeStackedOnBoard(CuttingBoardBlockEntity board) {
        if (!isLoaded(CULINARY_COMPAT)) {
            return List.of();
        }
        IItemHandlerModifiable extras;
        try {
            // Added to Farmer's Delight's board by Culinary Compat's mixin; read by name, none of its classes on our classpath.
            extras = board.getClass().getMethod("culinarycompat$getExtras").invoke(board) instanceof IItemHandlerModifiable handler ? handler : null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return List.of();
        }
        if (extras == null) {
            return List.of();
        }
        List<ItemStack> taken = new ArrayList<>();
        for (int slot = 0; slot < extras.getSlots(); slot++) {
            ItemStack stack = extras.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                taken.add(stack.copy());
                extras.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
        return taken;
    }

    @Override
    public void setItem(SkilletBlockEntity skillet, ItemStack stack) {
        ((IItemHandlerModifiable) skillet.getInventory()).setStackInSlot(0, stack);
    }

    @Override
    public ItemStack getItem(AbstractStoveBlockEntity stove, int slot) {
        return stove.getItems().getStackInSlot(slot);
    }

    @Override
    public void setItem(AbstractStoveBlockEntity stove, int slot, ItemStack stack) {
        stove.getItems().setStackInSlot(slot, stack);
    }

    @Override
    public boolean isLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isFarmersDelightAddon(String modId) {
        return ModList.get().getModContainerById(modId)
                .map(mod -> mod.getModInfo().getDependencies().stream()
                        .anyMatch(dependency -> dependency.isMandatory() && dependency.getModId().equals(FarmersDelightContent.MOD_ID)))
                .orElse(false);
    }

    @Override
    public void sendBoardDishes(ServerPlayer player) {
        ForgeNetwork.send(player);
    }

    @Override
    public boolean hasThisMod(ServerPlayer player) {
        return player instanceof FakePlayer || player.connection == null || ForgeNetwork.present(player);
    }

    @Override
    public boolean hasBoard(ServerPlayer player) {
        return player instanceof FakePlayer || player.connection == null || ForgeNetwork.hasBoard(player);
    }

    @Override
    public Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    /** Forge's ingredients, Farmer's Delight's tool action among them, test themselves. */
    @Override
    public Ingredient unwrap(Ingredient ingredient) {
        return ingredient;
    }
}
