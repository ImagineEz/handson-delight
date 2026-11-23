package com.pockyl.freeform_delight.forge.mixin;

import net.minecraft.network.Connection;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.pockyl.freeform_delight.HandsOnDelight;

/**
 * Culinary Compat sends its whole kitchen list in one packet while the datapacks go out to the players. With many Farmer's Delight
 * addons the list passes the 1 MiB a packet may hold, and the error it throws stops the rest of that sync: the tags, the recipes, the
 * cutting board's dishes and Culinary Compat's own Croptopia bridge never go out. Here the packet that does not fit is left out with a
 * warning, and everything after it goes out as always. Its client replaces the list with each packet, so it cannot be split.
 */
@Pseudo
@Mixin(targets = "io.github.smokahs.culinarycompat.network.Network", remap = false)
abstract class CulinaryBridgesSyncMixin {
    @Redirect(method = "sendBridges", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/network/simple/SimpleChannel;"
            + "sendTo(Ljava/lang/Object;Lnet/minecraft/network/Connection;Lnet/minecraftforge/network/NetworkDirection;)V"))
    private static void handson$sendToOne(SimpleChannel channel, Object message, Connection connection, NetworkDirection direction) {
        try {
            channel.sendTo(message, connection, direction);
        } catch (IllegalArgumentException tooLarge) {
            handson$leftOut(tooLarge);
        }
    }

    @Redirect(method = "sendBridgesToAll", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/network/simple/SimpleChannel;"
            + "send(Lnet/minecraftforge/network/PacketDistributor$PacketTarget;Ljava/lang/Object;)V"))
    private static void handson$sendToAll(SimpleChannel channel, PacketDistributor.PacketTarget target, Object message) {
        try {
            channel.send(target, message);
        } catch (IllegalArgumentException tooLarge) {
            handson$leftOut(tooLarge);
        }
    }

    @Unique
    private static void handson$leftOut(IllegalArgumentException tooLarge) {
        HandsOnDelight.LOGGER.warn("Culinary Compat's kitchen list is too large for one packet ({}): its recipe viewer pages for "
                + "Cooking for Blockheads' kitchen stay as they were, the rest of the sync goes on", tooLarge.getMessage());
    }
}
