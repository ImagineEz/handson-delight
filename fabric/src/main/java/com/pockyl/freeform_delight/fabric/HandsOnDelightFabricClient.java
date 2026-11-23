package com.pockyl.freeform_delight.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import com.pockyl.freeform_delight.HandsOnConfig;
import com.pockyl.freeform_delight.assembly.BoardDishes;
import com.pockyl.freeform_delight.client.HandsOnDelightClient;

/** Fabric entry point, client. */
public final class HandsOnDelightFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HandsOnDelightClient.init();
        PotModels.register();
        ClientPlayNetworking.registerGlobalReceiver(BoardDishes.CHANNEL,
                (client, handler, buffer, sender) -> BoardDishes.receive(BoardDishes.read(buffer)));
        if (!HandsOnConfig.cuttingBoard()) {
            // Named to the server: this client cuts on Farmer's Delight's board and crafts the board's dishes in its grid.
            ClientPlayNetworking.registerGlobalReceiver(BoardDishes.NO_BOARD, (client, handler, buffer, sender) -> {
            });
        }
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> HandsOnDelightClient.loggedOut());
    }
}
