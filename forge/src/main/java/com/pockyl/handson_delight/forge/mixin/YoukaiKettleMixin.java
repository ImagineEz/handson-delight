package com.pockyl.handson_delight.forge.mixin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import vectorwing.farmersdelight.common.block.entity.HeatableBlockEntity;

import com.pockyl.handson_delight.forge.YoukaiFluids;
import com.pockyl.handson_delight.station.KitchenKettle;
import com.pockyl.handson_delight.station.Youkai;

import java.util.ArrayList;
import java.util.List;

/**
 * Youkai's Feasts' kettle as a {@link KitchenKettle}: four leaves, one a slot, in a kettle of water (1000 mB) that comes to the boil
 * over heat, a tick a millibucket, and then brews for its recipe's time into a whole kettle of tea, poured a bottle (250 mB) at a
 * time. It brews as it always does; the station only reads it and uses it by hand as its own clicks do: its drink into its container,
 * a water bottle or a bucket in, and back out of a liquid that is no drink of it. Its classes build on the L2 libraries nested in its
 * jar, so it is reached by name, its slots and tank through its Forge capabilities. It has no lid of its own: the station's lid is
 * kept here, off only while somebody works at it.
 */
@Pseudo
@Mixin(targets = "dev.xkmc.youkaishomecoming.content.pot.kettle.KettleBlockEntity", remap = false)
abstract class YoukaiKettleMixin implements KitchenKettle {
    @Unique
    private static final ResourceLocation HANDSON$BREWING = Youkai.id("kettle");

    @Shadow
    private int heat;

    @Unique
    private boolean handson$open;
    @Unique
    private RecipeManager handson$readFrom;
    @Unique
    private List<Brewing> handson$brewing = List.of();

    @Unique
    private BlockEntity handson$self() {
        return (BlockEntity) (Object) this;
    }

    @Unique
    private IItemHandlerModifiable handson$items() {
        IItemHandler handler = handson$self().getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
        return handler instanceof IItemHandlerModifiable modifiable ? modifiable : null;
    }

    @Unique
    private IFluidHandler handson$tank() {
        return handson$self().getCapability(ForgeCapabilities.FLUID_HANDLER).orElse(null);
    }

    @Unique
    private FluidStack handson$inside() {
        IFluidHandler tank = handson$tank();
        return tank == null ? FluidStack.EMPTY : tank.getFluidInTank(0);
    }

    @Override
    public int handson$ingredients() {
        return MOST_INGREDIENTS;
    }

    @Override
    public int handson$perSlot() {
        return 1;
    }

    @Override
    public boolean handson$lidInBlock() {
        return false;
    }

    @Override
    public boolean handson$isOpen() {
        return handson$open;
    }

    @Override
    public void handson$setOpen(boolean open) {
        handson$open = open;
    }

    @Override
    public boolean handson$turnsOnTray() {
        return true;
    }

    @Override
    public boolean handson$boilsFirst() {
        return true;
    }

    @Override
    public float handson$boil() {
        FluidStack inside = handson$inside();
        return inside.isEmpty() || !inside.getFluid().is(FluidTags.WATER) ? -1.0F : Math.min(1.0F, (float) heat / inside.getAmount());
    }

    @Override
    public int handson$ticksToBoil() {
        FluidStack inside = handson$inside();
        return inside.isEmpty() || !inside.getFluid().is(FluidTags.WATER) ? 0 : Math.max(0, inside.getAmount() - heat);
    }

    @Override
    public ItemStack handson$getItem(int slot) {
        IItemHandlerModifiable items = handson$items();
        return items == null || slot >= items.getSlots() ? ItemStack.EMPTY : items.getStackInSlot(slot);
    }

    @Override
    public void handson$setItem(int slot, ItemStack stack) {
        IItemHandlerModifiable items = handson$items();
        if (items != null && slot < items.getSlots()) {
            items.setStackInSlot(slot, stack);
        }
    }

    @Override
    public Fluid handson$fluid() {
        FluidStack inside = handson$inside();
        return inside.isEmpty() ? Fluids.EMPTY : inside.getFluid();
    }

    @Override
    public int handson$amount() {
        return handson$inside().getAmount();
    }

    @Override
    public int handson$capacity() {
        IFluidHandler tank = handson$tank();
        return tank == null ? 1000 : tank.getTankCapacity(0);
    }

    @Override
    public ContainerData handson$data() {
        YoukaiTimedAccess timed = (YoukaiTimedAccess) this;
        return new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> timed.handson$recipeProgress();
                    case 1 -> timed.handson$totalTime();
                    default -> heat;
                };
            }

            @Override
            public void set(int index, int value) {
                if (index == 0) {
                    timed.handson$setRecipeProgress(value);
                } else if (index == 2) {
                    heat = value;
                }
            }

            @Override
            public int getCount() {
                return 3;
            }
        };
    }

    @Override
    public boolean handson$isHeated() {
        BlockEntity self = handson$self();
        return self.getLevel() != null && ((HeatableBlockEntity) this).isHeated(self.getLevel(), self.getBlockPos());
    }

    @Override
    public ItemStack handson$useOne(ItemStack held) {
        return YoukaiFluids.useOne(handson$tank(), held);
    }

    /** Its drink into the drink's container (a bottle of tea); out of plain water, an empty bucket or the like, as its own clicks do. */
    @Override
    public boolean handson$holdsMany(ItemStack stack) {
        return stack.is(net.minecraft.world.item.Items.BUCKET) || YoukaiFluids.isFlask(stack);
    }

    @Override
    public boolean handson$canServe(ItemStack stack) {
        return YoukaiFluids.canServe(handson$tank(), stack);
    }

    /** A water bottle, or anything a liquid is poured from (a water bucket), with room for it. */
    @Override
    public boolean handson$canPour(ItemStack stack) {
        return YoukaiFluids.canPour(handson$tank(), stack);
    }

    @Override
    public boolean handson$isVessel(ItemStack stack) {
        return YoukaiFluids.isVessel(stack, handson$recipes().stream().map(Brewing::result).toList());
    }

    @Override
    public ItemStack handson$drinkOf(Fluid fluid) {
        return YoukaiFluids.drinkOf(fluid);
    }

    @Override
    public int handson$tint(Fluid fluid) {
        return fluid == Fluids.EMPTY ? 0 : IClientFluidTypeExtensions.of(fluid).getTintColor();
    }

    /** Its brewing recipes, read once for each recipe manager (the book and the scene ask every frame). */
    @Override
    public List<Brewing> handson$recipes() {
        Level level = handson$self().getLevel();
        if (level == null) {
            return List.of();
        }
        RecipeManager manager = level.getRecipeManager();
        if (manager == handson$readFrom) {
            return handson$brewing;
        }
        List<Brewing> recipes = new ArrayList<>();
        RecipeType<?> type = BuiltInRegistries.RECIPE_TYPE.get(HANDSON$BREWING);
        for (Recipe<?> recipe : manager.getRecipes()) {
            if (type == null || recipe.getType() != type || !(recipe instanceof YoukaiKettleRecipeAccess brewing)) {
                continue;
            }
            FluidStack result = brewing.handson$result();
            List<Ingredient> input = brewing.handson$input();
            if (result == null || result.isEmpty() || input == null || input.isEmpty()) {
                continue;
            }
            // Its kettle brews only a full kettle of boiling water.
            recipes.add(new Brewing(recipe.getId(), List.copyOf(input), Fluids.WATER, 1000, result.getFluid(), brewing.handson$time()));
        }
        handson$readFrom = manager;
        handson$brewing = List.copyOf(recipes);
        return handson$brewing;
    }

    /** Its kettle gives nothing for a drink but the drink. */
    @Override
    public void handson$awardRecipes(Player player) {
    }

    @Override
    public BlockEntity handson$blockEntity() {
        return handson$self();
    }
}
