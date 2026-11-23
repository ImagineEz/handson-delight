package com.pockyl.freeform_delight.forge;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraft.world.item.Item;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import com.pockyl.freeform_delight.HandsOnDelight;
import com.pockyl.freeform_delight.station.FreeformMeal;

/** Forge entry point. */
@Mod(HandsOnDelight.MOD_ID)
public final class HandsOnDelightForge {
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, HandsOnDelight.MOD_ID);

    static {
        ITEMS.register(FreeformMeal.ID, FreeformMeal::new);
    }

    public HandsOnDelightForge() {
        ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        HandsOnDelight.init();
        ForgeNetwork.register();
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ForgeClient::init);
    }
}
