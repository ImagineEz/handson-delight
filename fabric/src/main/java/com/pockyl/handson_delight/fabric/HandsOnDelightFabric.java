package com.pockyl.handson_delight.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.S2CPlayChannelEvents;

import com.pockyl.handson_delight.HandsOnDelight;
import com.pockyl.handson_delight.assembly.BoardDishes;
import com.pockyl.handson_delight.assembly.BookDishes;

/** Fabric entry point, both sides. */
public final class HandsOnDelightFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        HandsOnDelight.init();
        // A client names its channels only after it joined, its recipe book already sent: one with the mod forgets the board's dishes now.
        S2CPlayChannelEvents.REGISTER.register((handler, sender, server, channels) -> {
            if (channels.contains(BoardDishes.CHANNEL)) {
                server.execute(() -> BookDishes.forget(handler.player));
            }
        });
    }
}
