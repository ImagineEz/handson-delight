package com.pockyl.handson_delight.forge.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import umpaz.farmersrespite.common.block.entity.KettleBlockEntity;
import umpaz.farmersrespite.common.crafting.KettlePouringRecipe;
import umpaz.farmersrespite.common.crafting.KettleRecipe;
import umpaz.farmersrespite.common.registry.FRRecipeTypes;

import com.pockyl.handson_delight.HandsOnConfig;
import com.pockyl.handson_delight.station.KitchenKettle;

import java.util.ArrayList;
import java.util.List;

/**
 * Farmer's Respite's kettle as a {@link KitchenKettle}: it brews as it always does, the station only reads it and uses it by hand,
 * through the kettle's own {@code fluidExtract} for what goes in and out.
 */
@Pseudo
@Mixin(value = KettleBlockEntity.class, remap = false)
abstract class KettleMixin implements KitchenKettle {
    @Shadow
    @Final
    private ItemStackHandler inventory;
    @Shadow
    @Final
    private FluidTank fluidTank;
    @Shadow
    @Final
    protected ContainerData kettleData;
    @Shadow
    private boolean checkNewRecipe;

    @Shadow
    public abstract ItemStack fluidExtract(KettleBlockEntity kettle, ItemStack slotIn, ItemStack slotOut);

    /** A drink takes as long as the config's {@code kettle_time} makes it, however it got into the kettle. */
    @Redirect(method = "processBrewing", at = @At(value = "INVOKE",
            target = "Lumpaz/farmersrespite/common/crafting/KettleRecipe;getBrewTime()I"))
    private int handson$brewTime(KettleRecipe recipe) {
        return HandsOnConfig.scaled(recipe.getBrewTime(), HandsOnConfig.kettleTime());
    }

    @Unique
    private KettleBlockEntity handson$self() {
        return (KettleBlockEntity) (Object) this;
    }

    @Override
    public ItemStack handson$getItem(int slot) {
        return inventory.getStackInSlot(slot);
    }

    @Override
    public void handson$setItem(int slot, ItemStack stack) {
        inventory.setStackInSlot(slot, stack);
    }

    @Override
    public Fluid handson$fluid() {
        return fluidTank.isEmpty() ? Fluids.EMPTY : fluidTank.getFluid().getFluid();
    }

    @Override
    public int handson$amount() {
        return fluidTank.getFluidAmount();
    }

    @Override
    public int handson$capacity() {
        return fluidTank.getCapacity();
    }

    @Override
    public ContainerData handson$data() {
        return kettleData;
    }

    @Override
    public boolean handson$isHeated() {
        return handson$self().isHeated();
    }

    @Override
    public ItemStack handson$useOne(ItemStack held) {
        if (held.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack one = held.copyWithCount(1);
        ItemStack back = fluidExtract(handson$self(), one, ItemStack.EMPTY);
        if (back.isEmpty()) {
            return ItemStack.EMPTY;
        }
        held.shrink(1);
        // The kettle looks for a recipe again only when its ingredients change: leaves put in before the water would never brew.
        checkNewRecipe = true;
        return back;
    }

    @Override
    public boolean handson$canServe(ItemStack stack) {
        if (stack.isEmpty() || fluidTank.isEmpty()) {
            return false;
        }
        for (KettlePouringRecipe pouring : handson$pourings()) {
            if (pouring.getFluid().isSame(fluidTank.getFluid().getFluid()) && stack.is(pouring.getContainer().getItem())) {
                return fluidTank.getFluidAmount() >= pouring.getAmount();
            }
        }
        IFluidHandlerItem handler = handson$handlerOf(stack);
        return handler != null && (handler.getTanks() == 0 || handler.getFluidInTank(0).isEmpty())
                && handler.fill(fluidTank.getFluid().copy(), IFluidHandler.FluidAction.SIMULATE) > 0;
    }

    @Override
    public boolean handson$canPour(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        for (KettlePouringRecipe pouring : handson$pourings()) {
            if (stack.is(pouring.getOutput().getItem()) && (fluidTank.isEmpty() || pouring.getFluid().isSame(fluidTank.getFluid().getFluid()))) {
                // A drink poured back: the same liquid, with room for it.
                return fluidTank.getFluidAmount() + pouring.getAmount() <= fluidTank.getCapacity();
            }
        }
        IFluidHandlerItem handler = handson$handlerOf(stack);
        if (handler == null || handler.getTanks() == 0 || handler.getFluidInTank(0).isEmpty()) {
            return false;
        }
        FluidStack inside = handler.getFluidInTank(0);
        return (fluidTank.isEmpty() || fluidTank.getFluid().isFluidEqual(inside))
                && fluidTank.getFluidAmount() + inside.getAmount() <= fluidTank.getCapacity();
    }

    @Override
    public boolean handson$isVessel(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (handson$handlerOf(stack) != null) {
            return true;
        }
        return handson$pourings().stream().anyMatch(pouring -> stack.is(pouring.getContainer().getItem()) || stack.is(pouring.getOutput().getItem()));
    }

    @Override
    public ItemStack handson$drinkOf(Fluid fluid) {
        if (fluid == Fluids.EMPTY) {
            return ItemStack.EMPTY;
        }
        ItemStack drink = ItemStack.EMPTY;
        for (KettlePouringRecipe pouring : handson$pourings()) {
            if (pouring.getFluid().isSame(fluid) && !pouring.getOutput().isEmpty()) {
                // A bottle before a cup of another mod, as the kettle's own drinks come.
                if (drink.isEmpty() || pouring.getContainer().is(Items.GLASS_BOTTLE)) {
                    drink = pouring.getOutput().copy();
                }
            }
        }
        if (!drink.isEmpty()) {
            return drink;
        }
        return fluid.getBucket() == Items.AIR ? ItemStack.EMPTY : new ItemStack(fluid.getBucket());
    }

    @Override
    public int handson$tint(Fluid fluid) {
        return fluid == Fluids.EMPTY ? 0 : IClientFluidTypeExtensions.of(fluid).getTintColor();
    }

    @Override
    public List<Brewing> handson$recipes() {
        List<Brewing> recipes = new ArrayList<>();
        Level level = handson$self().getLevel();
        if (level == null) {
            return recipes;
        }
        for (KettleRecipe recipe : level.getRecipeManager().getAllRecipesFor(FRRecipeTypes.BREWING.get())) {
            FluidStack base = recipe.getFluidIn();
            FluidStack result = recipe.getFluidOut();
            if (base == null || base.isEmpty() || result == null || result.isEmpty()) {
                continue;
            }
            recipes.add(new Brewing(recipe.getId(), List.copyOf(recipe.getIngredients()), base.getFluid(), base.getAmount(), result.getFluid(),
                    recipe.getBrewTime()));
        }
        return recipes;
    }

    @Override
    public void handson$awardRecipes(Player player) {
        handson$self().awardUsedRecipes(player, List.of());
    }

    @Override
    public BlockEntity handson$blockEntity() {
        return handson$self();
    }

    @Unique
    private List<KettlePouringRecipe> handson$pourings() {
        Level level = handson$self().getLevel();
        return level == null ? List.of() : level.getRecipeManager().getAllRecipesFor(FRRecipeTypes.KETTLE_POURING.get());
    }

    @Unique
    private static IFluidHandlerItem handson$handlerOf(ItemStack stack) {
        return stack.copyWithCount(1).getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
    }
}
