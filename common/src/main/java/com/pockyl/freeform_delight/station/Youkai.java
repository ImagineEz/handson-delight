package com.pockyl.freeform_delight.station;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import com.pockyl.freeform_delight.platform.Platform;

/**
 * The Youkai kitchen: Youkai's Feasts is the food of Youkai's Homecoming on its own, the same code in the same package under another
 * mod id, so a pack has one of them, never both. Their kettle, cuisine board, pots, steamer and fermentation tank are the same classes
 * and work the same; their blocks, items, recipe types and textures differ only in the namespace, which is the one of the mod that is
 * there. Every Youkai station names its things through it.
 */
public final class Youkai {
    public static final String FEASTS = "youkaisfeasts";
    public static final String HOMECOMING = "youkaishomecoming";
    /** The namespace of the Youkai mod in the game: Youkai's Feasts' when neither is there, so that nothing matches. */
    public static final String MOD_ID = !Platform.INSTANCE.isLoaded(FEASTS) && Platform.INSTANCE.isLoaded(HOMECOMING) ? HOMECOMING : FEASTS;

    private Youkai() {
    }

    /** Whether one of the two is in the game. */
    public static boolean isLoaded() {
        return Platform.INSTANCE.isLoaded(MOD_ID);
    }

    /** A thing of the Youkai mod in the game. */
    @SuppressWarnings("removal")
    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    public static Block block(String path) {
        return BuiltInRegistries.BLOCK.get(id(path));
    }

    public static Item item(String path) {
        return BuiltInRegistries.ITEM.get(id(path));
    }
}
