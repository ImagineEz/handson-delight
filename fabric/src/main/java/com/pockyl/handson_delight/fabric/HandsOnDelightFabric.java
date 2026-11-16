package com.pockyl.handson_delight.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.S2CPlayChannelEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

import com.pockyl.handson_delight.HandsOnDelight;
import com.pockyl.handson_delight.assembly.BoardDishes;
import com.pockyl.handson_delight.assembly.BookDishes;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Fabric entry point, both sides. */
public final class HandsOnDelightFabric implements ModInitializer {
    /**
     * Connections whose client named the board's channel, served on the next server tick. Channels named while logging in are
     * announced from inside the connection's constructor, before Fabric can tell what may be sent to it: asking then crashes the
     * join (`server.execute` runs at once on the server thread).
     */
    private static final Set<ServerGamePacketListenerImpl> READY = ConcurrentHashMap.newKeySet();

    @Override
    public void onInitialize() {
        HandsOnDelight.init();
        // A client names its channels only after it joined, its recipe book already sent: one with the mod forgets the board's dishes then.
        S2CPlayChannelEvents.REGISTER.register((handler, sender, server, channels) -> {
            if (channels.contains(BoardDishes.CHANNEL)) {
                READY.add(handler);
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (READY.isEmpty()) {
                return;
            }
            List<ServerGamePacketListenerImpl> ready = new ArrayList<>(READY);
            READY.removeAll(ready);
            for (ServerGamePacketListenerImpl handler : ready) {
                if (handler.player.connection == handler && !handler.player.hasDisconnected()) {
                    BookDishes.forget(handler.player);
                }
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> READY.remove(handler));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> READY.clear());
    }
}
