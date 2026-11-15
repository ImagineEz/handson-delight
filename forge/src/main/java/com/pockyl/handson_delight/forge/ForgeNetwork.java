package com.pockyl.handson_delight.forge;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import com.pockyl.handson_delight.HandsOnConfig;
import com.pockyl.handson_delight.assembly.BoardDishes;

import java.util.function.Supplier;

/** The one packet of the mod: the board's dishes, for recipe viewers. Either side may lack the mod; the channel tells. */
final class ForgeNetwork {
    private static final String VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(BoardDishes.CHANNEL, () -> VERSION,
            NetworkRegistry.acceptMissingOr(VERSION), NetworkRegistry.acceptMissingOr(VERSION));
    /**
     * {@link BoardDishes#NO_BOARD}: a dedicated server always has it, to see which clients name it; a game client has it only with
     * the board's station off. Null in a game whose board is on: its own server and the players of a LAN game count as having it.
     */
    private static SimpleChannel noBoard;

    private ForgeNetwork() {
    }

    static void register() {
        CHANNEL.messageBuilder(Dishes.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((dishes, buffer) -> BoardDishes.write(buffer))
                .decoder(buffer -> new Dishes(BoardDishes.read(buffer)))
                .consumerNetworkThread(ForgeNetwork::handle)
                .add();
        if (FMLEnvironment.dist.isDedicatedServer() || !HandsOnConfig.cuttingBoard()) {
            noBoard = NetworkRegistry.newSimpleChannel(BoardDishes.NO_BOARD, () -> VERSION, NetworkRegistry.acceptMissingOr(VERSION),
                    NetworkRegistry.acceptMissingOr(VERSION));
        }
    }

    /** Whether the client has the channel; false for a connection without a network channel (another mod's mock player). */
    static boolean present(ServerPlayer player) {
        Connection connection = player.connection.connection;
        return connection != null && connection.channel() != null && CHANNEL.isRemotePresent(connection);
    }

    /** Whether the client has the mod and does not name {@link BoardDishes#NO_BOARD}. */
    static boolean hasBoard(ServerPlayer player) {
        return present(player) && (noBoard == null || !noBoard.isRemotePresent(player.connection.connection));
    }

    static void send(ServerPlayer player) {
        if (present(player)) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Dishes(null));
        }
    }

    private static void handle(Dishes dishes, Supplier<NetworkEvent.Context> context) {
        BoardDishes.receive(dishes.book());
        context.get().setPacketHandled(true);
    }

    /** Written from {@code AssemblyRecipes.book()}: the server sends no other list. */
    private record Dishes(CompoundTag book) {
    }
}
