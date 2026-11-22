package com.pockyl.handson_delight.forge.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.items.ItemStackHandler;
import net.ribs.vintagedelight.block.entity.FermentingJarBlockEntity;
import net.ribs.vintagedelight.item.ModTags;
import net.ribs.vintagedelight.recipe.FermentingRecipe;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import com.pockyl.handson_delight.station.KegStation;
import com.pockyl.handson_delight.station.KitchenKeg;

import java.util.ArrayList;
import java.util.List;

/**
 * Vintage Delight's fermenting jar as a {@link KitchenKeg} with no liquid of its own: it ferments what lies in its six slots as it always
 * does, the station only reads it and uses it by hand. The bottle or bowl some of its recipes take from its container slot is one more
 * thing dropped in at the station, the last of its ingredients, and is put in that slot once the food is laid out. Its two result slots
 * are seen by the station as one. It always stands full of its brine, which the station shows as a liquid things float on.
 */
@Pseudo
@Mixin(value = FermentingJarBlockEntity.class, remap = false)
abstract class VintageJarMixin implements KitchenKeg {
    @Unique
    private static final int HANDSON$INPUTS = 6;
    @Unique
    private static final int HANDSON$CONTAINER = 6;
    @Unique
    private static final int HANDSON$FIRST_RESULT = 7;
    @Unique
    private static final int HANDSON$RESULTS = 2;
    /** The brine its model always shows in it: the colour of its contents texture. */
    @Unique
    private static final int HANDSON$BRINE = 0x8C528185;

    @Shadow
    @Final
    private ItemStackHandler itemHandler;
    @Shadow
    private int progress;
    @Shadow
    private int maxProgress;

    @Unique
    private RecipeManager handson$readFrom;
    @Unique
    private List<Fermenting> handson$fermenting = List.of();

    @Unique
    private FermentingJarBlockEntity handson$self() {
        return (FermentingJarBlockEntity) (Object) this;
    }

    @Override
    public int handson$ingredients() {
        return KegStation.JAR_INGREDIENTS;
    }

    @Override
    public int handson$outputSlot() {
        return HANDSON$FIRST_RESULT;
    }

    @Override
    public boolean handson$feelsWarmth() {
        return false;
    }

    /** Any use of the jar opens its menu: the station opens with whatever is in the hand. */
    @Override
    public boolean handson$quickUse() {
        return false;
    }

    @Override
    public int handson$brine() {
        return HANDSON$BRINE;
    }

    @Override
    public ItemStack handson$getItem(int slot) {
        if (slot <= HANDSON$CONTAINER) {
            return itemHandler.getStackInSlot(slot);
        }
        return slot == HANDSON$FIRST_RESULT ? itemHandler.getStackInSlot(handson$resultSlot()) : ItemStack.EMPTY;
    }

    @Override
    public void handson$setItem(int slot, ItemStack stack) {
        if (slot <= HANDSON$CONTAINER) {
            itemHandler.setStackInSlot(slot, stack);
        } else if (slot == HANDSON$FIRST_RESULT) {
            itemHandler.setStackInSlot(handson$resultSlot(), stack);
        }
    }

    /** The result slot the station sees: the first that holds something, else the first. */
    @Unique
    private int handson$resultSlot() {
        for (int slot = HANDSON$FIRST_RESULT; slot < HANDSON$FIRST_RESULT + HANDSON$RESULTS; slot++) {
            if (!itemHandler.getStackInSlot(slot).isEmpty()) {
                return slot;
            }
        }
        return HANDSON$FIRST_RESULT;
    }

    /**
     * A bottle or bowl (its container tag) dropped in among the food goes to its container slot, where its recipes take it from, and
     * anything else that got into that slot goes to a free place among the food: its recipes never read such a thing there, and it
     * would take one of it with every thing it makes.
     */
    @Override
    public void handson$settle() {
        ItemStack held = itemHandler.getStackInSlot(HANDSON$CONTAINER);
        boolean stray = !held.isEmpty() && !held.is(ModTags.CONTAINER_ITEMS);
        for (int slot = 0; slot < HANDSON$INPUTS; slot++) {
            ItemStack stack = itemHandler.getStackInSlot(slot);
            if (stack.is(ModTags.CONTAINER_ITEMS) && (held.isEmpty() || stray)) {
                itemHandler.setStackInSlot(slot, held);
                itemHandler.setStackInSlot(HANDSON$CONTAINER, stack);
                return;
            }
            if (stack.is(ModTags.CONTAINER_ITEMS) && ItemStack.isSameItemSameTags(stack, held) && held.getCount() < held.getMaxStackSize()) {
                int moved = Math.min(stack.getCount(), held.getMaxStackSize() - held.getCount());
                itemHandler.setStackInSlot(HANDSON$CONTAINER, held.copyWithCount(held.getCount() + moved));
                itemHandler.setStackInSlot(slot, stack.getCount() == moved ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - moved));
                return;
            }
        }
        for (int slot = 0; stray && slot < HANDSON$INPUTS; slot++) {
            if (itemHandler.getStackInSlot(slot).isEmpty()) {
                itemHandler.setStackInSlot(slot, held);
                itemHandler.setStackInSlot(HANDSON$CONTAINER, ItemStack.EMPTY);
                return;
            }
        }
    }

    @Override
    public Fluid handson$fluid() {
        return Fluids.EMPTY;
    }

    @Override
    public int handson$amount() {
        return 0;
    }

    @Override
    public int handson$capacity() {
        return 1000;
    }

    /** Its brine fills it, whatever lies in it. */
    @Override
    public int handson$shownAmount() {
        return handson$capacity();
    }

    /** Its progress in ticks; the time of the recipe it holds only while it ferments one (it keeps the last one's otherwise). */
    @Override
    public ContainerData handson$data() {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> progress;
                    case 1 -> progress > 0 ? maxProgress : 0;
                    case 2 -> 3;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                if (index == 0) {
                    progress = value;
                }
            }

            @Override
            public int getCount() {
                return 3;
            }
        };
    }

    @Override
    public void handson$feelTemperature() {
    }

    @Override
    public int handson$wantedTemperature() {
        return 0;
    }

    /** No liquid goes in or out. */
    @Override
    public List<ItemStack> handson$use(ItemStack held, int most, boolean creative) {
        return List.of();
    }

    @Override
    public boolean handson$canTap(ItemStack stack) {
        return false;
    }

    @Override
    public boolean handson$canPour(ItemStack stack) {
        return false;
    }

    /** Nothing is poured or filled here: a bottle of vinegar or milk is what ferments, an empty one what the brew is put in. */
    @Override
    public boolean handson$isVessel(ItemStack stack) {
        return false;
    }

    @Override
    public ItemStack handson$drinkOf(Fluid fluid) {
        return ItemStack.EMPTY;
    }

    /**
     * Its fermenting recipes, each with the container it takes as its last ingredient; read once for each recipe manager. Its recipe type
     * is in no registry, so they are found by the type object itself, as the jar finds them.
     */
    @Override
    public List<Fermenting> handson$recipes() {
        Level level = handson$self().getLevel();
        if (level == null) {
            return List.of();
        }
        RecipeManager manager = level.getRecipeManager();
        if (manager == handson$readFrom) {
            return handson$fermenting;
        }
        List<Fermenting> recipes = new ArrayList<>();
        for (FermentingRecipe recipe : manager.getAllRecipesFor(FermentingRecipe.Type.INSTANCE)) {
            ItemStack result = recipe.getResultItem(level.registryAccess()).copy();
            List<Ingredient> ingredients = new ArrayList<>(recipe.getIngredients());
            if (result.isEmpty() || ingredients.isEmpty() || ingredients.size() > HANDSON$INPUTS) {
                continue;
            }
            ItemStack container = recipe.getContainerItemStack();
            if (!container.isEmpty()) {
                ingredients.add(Ingredient.of(container));
            }
            recipes.add(new Fermenting(recipe.getId(), List.copyOf(ingredients), Fluids.EMPTY, 0, Fluids.EMPTY, result,
                    Math.max(1, recipe.getProcessingTime()), 0));
        }
        handson$readFrom = manager;
        handson$fermenting = List.copyOf(recipes);
        return handson$fermenting;
    }

    /** Its menu's result slots give nothing but the thing. */
    @Override
    public void handson$awardRecipes(Player player) {
    }

    @Override
    public BlockEntity handson$blockEntity() {
        return handson$self();
    }
}
