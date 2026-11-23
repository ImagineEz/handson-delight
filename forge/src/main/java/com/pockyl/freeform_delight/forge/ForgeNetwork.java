package com.pockyl.freeform_delight.forge;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.ConnectionData;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import com.pockyl.freeform_delight.HandsOnConfig;
import com.pockyl.freeform_delight.assembly.BoardDishes;

import java.util.function.Supplier;

/** The one packet of the mod: the board's dishes, for recipe viewers. Either side may lack the mod; the channel tells. */
final class ForgeNetwork {
    private static final String VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(BoardDishes.CHANNEL, () -> VERSION,
            NetworkRegistry.acceptMissingOr(VERSION), NetworkRegistry.acceptMissingOr(VERSION));
    /**
     * {@link BoardDishes#NO_BOARD}: a game client has it only with the board's station off, and names it to the server it joins; a
     * dedicated server has it too. The server reads it from the channels the client named, which a LAN host whose own board is on
     * has too, though its game never made the channel.
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
        return present(player) && !namesNoBoard(player.connection.connection);
    }

    /**
     * Whether the client named {@link BoardDishes#NO_BOARD}. Asking the channel itself, a LAN host whose board is on (it never made
     * the channel) counted every guest as having the board, so a guest with the board off lost the dishes from the crafting grid.
     */
    private static boolean namesNoBoard(Connection connection) {
        ConnectionData data = NetworkHooks.getConnectionData(connection);
        if (data != null) {
            return data.getChannels().containsKey(BoardDishes.NO_BOARD);
        }
        return noBoard != null && noBoard.isRemotePresent(connection);
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
