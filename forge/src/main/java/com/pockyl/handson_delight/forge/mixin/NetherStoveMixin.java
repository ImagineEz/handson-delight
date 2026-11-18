package com.pockyl.handson_delight.forge.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import umpaz.nethersdelight.common.block.entity.AbstractStoveBlockEntity;

import com.pockyl.handson_delight.HandsOnDelight;
import com.pockyl.handson_delight.station.Grill;
import com.pockyl.handson_delight.station.StoveCooking;
import com.pockyl.handson_delight.station.StoveTop;

/**
 * Nether's Delight's blackstone stove is a stove station: its block entity is a copy of an older Farmer's Delight stove of its own,
 * six places at the same offsets, campfire recipes, the same saved keys. The grill state lives in it as in Farmer's Delight's
 * ({@code StoveBlockEntityMixin}). Its cooking throws done food into the world with no hook to catch it, so a piece the station
 * grilled through is finished by the station itself, as the campfire makes it; food put on its own way cooks its way.
 */
@Pseudo
@Mixin(value = AbstractStoveBlockEntity.class, remap = false)
abstract class NetherStoveMixin implements StoveTop {
    @Unique
    private static final String HANDSON$GRILL = HandsOnDelight.MOD_ID + ":grill";
    /** Its own key for the cooking times of the six places, as Farmer's Delight's. */
    @Unique
    private static final String HANDSON$TOTAL_TIMES = "CookingTotalTimes";

    @Shadow
    @Final
    private NonNullList<ItemStack> items;

    @Shadow
    @Final
    private int[] cookingProgress;

    @Shadow
    @Final
    private int[] cookingTime;

    @Shadow
    public abstract boolean isBlockedAbove();

    @Unique
    private final Grill handson$grill = new Grill();

    @Unique
    private final boolean[] handson$cookingNow = new boolean[Grill.PLACES];

    @Override
    public Grill handson$grill() {
        return handson$grill;
    }

    @Override
    public int[] handson$progress() {
        return cookingProgress;
    }

    @Override
    public int[] handson$times() {
        return cookingTime;
    }

    @Override
    public boolean[] handson$cookingNow() {
        return handson$cookingNow;
    }

    @Override
    public ItemStack handson$item(int slot) {
        return slot >= 0 && slot < items.size() ? items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public void handson$setItem(int slot, ItemStack stack) {
        if (slot >= 0 && slot < items.size()) {
            items.set(slot, stack);
        }
    }

    @Override
    public int handson$slots() {
        return items.size();
    }

    @Override
    public boolean handson$covered() {
        return isBlockedAbove();
    }

    @Override
    public boolean handson$cooksForTheStation() {
        return false;
    }

    /** Lit: the pieces laid by hand grill before its own count runs, which is held at nothing for them. */
    @Inject(method = "cookTick", at = @At("HEAD"))
    private static void handson$grillByHand(Level level, BlockPos pos, BlockState state, AbstractStoveBlockEntity<?, ?> stove,
            CallbackInfo callback) {
        StoveCooking.tick(stove);
    }

    /** Put out: nothing grills, but a place emptied meanwhile still starts over. */
    @Inject(method = "cooldownTick", at = @At("HEAD"))
    private static void handson$coolByHand(Level level, BlockPos pos, BlockState state, AbstractStoveBlockEntity<?, ?> stove,
            CallbackInfo callback) {
        StoveCooking.tick(stove);
    }

    /** Food put on with its own right click is its to cook: it takes the first free place. */
    @Inject(method = "placeFood", at = @At("HEAD"))
    private void handson$itsOwnWay(Entity entity, ItemStack stack, int ticks, CallbackInfoReturnable<Boolean> callback) {
        int places = Math.min(Grill.PLACES, items.size());
        for (int place = 0; place < places; place++) {
            if (items.get(place).isEmpty()) {
                handson$grill.farmersDelightsWay(place);
                return;
            }
        }
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"), remap = true)
    private void handson$saveGrill(CompoundTag tag, CallbackInfo callback) {
        CompoundTag grill = new CompoundTag();
        handson$grill.save(grill);
        if (!grill.isEmpty()) {
            tag.put(HANDSON$GRILL, grill);
            tag.putIntArray(HANDSON$TOTAL_TIMES, StoveCooking.savedTimes((AbstractStoveBlockEntity<?, ?>) (Object) this));
        }
    }

    @Inject(method = "getUpdateTag", at = @At("RETURN"), remap = true)
    private void handson$sendGrill(CallbackInfoReturnable<CompoundTag> callback) {
        CompoundTag grill = new CompoundTag();
        handson$grill.save(grill);
        callback.getReturnValue().put(HANDSON$GRILL, grill);
    }

    @Inject(method = "load", at = @At("TAIL"), remap = true)
    private void handson$loadGrill(CompoundTag tag, CallbackInfo callback) {
        handson$grill.load(tag.getCompound(HANDSON$GRILL));
    }
}
