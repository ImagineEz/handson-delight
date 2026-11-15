package com.pockyl.handson_delight.forge;

import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import com.pockyl.handson_delight.client.HandsOnDelightClient;

/** Client setup on Forge. */
final class ForgeClient {
    private ForgeClient() {
    }

    static void init() {
        HandsOnDelightClient.init();
        FMLJavaModLoadingContext.get().getModEventBus().addListener(PotPack::add);
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> HandsOnDelightClient.loggedOut());
    }
}
