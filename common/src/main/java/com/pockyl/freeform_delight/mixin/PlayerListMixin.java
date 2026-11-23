package com.pockyl.freeform_delight.mixin;

import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.pockyl.freeform_delight.assembly.AssemblyRecipes;
import com.pockyl.freeform_delight.assembly.BoardDishes;
import com.pockyl.freeform_delight.platform.Platform;

import java.util.List;

/**
 * Tells clients which crafting recipes are put together on the board, just before the recipes themselves go out: JEI and EMI read
 * them as soon as the recipes and tags arrive, and a later packet would find them already loaded. Loader events come after that.
 */
@Mixin(PlayerList.class)
abstract class PlayerListMixin {
    @Shadow
    @Final
    private List<ServerPlayer> players;

    @Inject(method = "placeNewPlayer", at = @At(value = "NEW",
            target = "net/minecraft/network/protocol/game/ClientboundUpdateRecipesPacket"))
    private void handson$boardDishesOnJoin(Connection connection, ServerPlayer player, CallbackInfo callback) {
        BoardDishes.prepare();
        Platform.INSTANCE.sendBoardDishes(player);
    }

    /**
     * After a reload, just before the tags go out: after Forge's datapack sync, where Culinary Compat adds its cuts of several things
     * anew, so the viewers get those too without a rejoin.
     */
    @Inject(method = "reloadResources", at = @At(value = "NEW", target = "net/minecraft/network/protocol/game/ClientboundUpdateTagsPacket"))
    private void handson$boardDishesOnReload(CallbackInfo callback) {
        // The tags were bound again after the recipes loaded: the board's dishes are chosen with the new ones.
        AssemblyRecipes.tagsChanged();
        BoardDishes.prepare();
        for (ServerPlayer player : players) {
            Platform.INSTANCE.sendBoardDishes(player);
        }
    }
}
