package com.pockyl.handson_delight.forge;

import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.fml.ModList;

import com.pockyl.handson_delight.HandsOnConfig;
import com.pockyl.handson_delight.HandsOnDelight;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;

/**
 * The cooking pot in the world is this mod's pot: its body without Farmer's Delight's spoon, which the station moves, and with a
 * floor deep enough for the soup and what floats in it. The block state file that says so lies under Farmer's Delight's own name,
 * and a resource pack that remodels Farmer's Delight (Excalibur) sits above every mod: its file won, the pot covered the soup
 * and kept a second spoon that does nothing. This mod's file therefore also comes as a hidden built-in pack fixed above all
 * others, holding that one file; a pack's textures still apply to the pot's models. Miner's Delight's copper pot comes the same way:
 * open, its spoon in the station's hand, and this mod is not ordered after Miner's Delight, so only the pack is sure to win; so does
 * Dungeons Delight's monster pot. Farmer's Respite's kettle comes
 * the same way: its opening under the lid is a real one, and its tray is Farmer's Delight's heating tray, where its own model names
 * tray textures Farmer's Delight no longer has. Farmer's Spell's alchemist pot comes the same way: its dark top is a real opening;
 * so does Youkai's Feasts' kettle: its painted hole is a real opening under its lid, which the station lifts.
 * <p>
 * Vanilla Delight dyes the pot: it empties the block's models and draws the pot from its block entity with its own models, one
 * per colour and support, which are Farmer's Delight's pot again. This mod's versions of them (its pot in Vanilla Delight's
 * colours) come in the same pack, so the dye stays and the spoon and floor are this mod's.
 * <p>
 * The files lie outside the mod's own {@code assets}, so only this pack shows them, and it shows each only while its station is on:
 * a pot whose station a modpack turned off is the other mod's pot again, with its own spoon, since no station draws one.
 */
final class PotPack {
    private static final String ID = HandsOnDelight.MOD_ID + "_pot";
    /** The folder of the mod's files that holds the pack's {@code assets}. */
    private static final String FOLDER = "pot_pack";
    /** Each block state file and the station that wants it. */
    @SuppressWarnings("removal")
    private static final Map<ResourceLocation, BooleanSupplier> STATES = Map.of(
            new ResourceLocation("farmersdelight", "blockstates/cooking_pot.json"), HandsOnConfig::cookingPot,
            new ResourceLocation("miners_delight", "blockstates/copper_pot.json"), HandsOnConfig::cookingPot,
            new ResourceLocation("dungeonsdelight", "blockstates/monster_pot.json"), HandsOnConfig::cookingPot,
            new ResourceLocation("farmersrespite", "blockstates/kettle.json"), HandsOnConfig::kettle,
            new ResourceLocation("farmers_spell", "blockstates/alchemist_pot.json"), HandsOnConfig::cookingPot,
            new ResourceLocation("youkaisfeasts", "blockstates/kettle.json"), HandsOnConfig::kettle);
    private static final String DYED_NAMESPACE = "vanilladelight";
    private static final String DYED_MODELS = "models/block/cooking_pot_";
    private static final Set<String> NAMESPACES = Set.of("farmersdelight", "miners_delight", "dungeonsdelight", "farmersrespite",
            "farmers_spell", "youkaisfeasts", DYED_NAMESPACE);

    private PotPack() {
    }

    static void add(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES || !HandsOnConfig.cookingPot() && !HandsOnConfig.kettle()) {
            return;
        }
        Path root = ModList.get().getModFileById(HandsOnDelight.MOD_ID).getFile().getSecureJar().getRootPath().resolve(FOLDER);
        Pack.ResourcesSupplier resources = id -> new OnlyThePot(id, root);
        int format = SharedConstants.getCurrentVersion().getPackVersion(PackType.CLIENT_RESOURCES);
        Pack.Info info = new Pack.Info(Component.literal("Hands-On Delight: cooking pot"), format, format, FeatureFlagSet.of(), true);
        Pack pack = Pack.create(ID, Component.literal("Hands-On Delight: cooking pot"), true, resources, info, PackType.CLIENT_RESOURCES,
                Pack.Position.TOP, true, PackSource.BUILT_IN);
        event.addRepositorySource(consumer -> consumer.accept(pack));
    }

    /** The pack's files, of which only the block states and dyed pots' models of stations that are on are shown. */
    private static final class OnlyThePot extends PathPackResources {
        OnlyThePot(String id, Path root) {
            super(id, root, true);
        }

        @Override
        public IoSupplier<InputStream> getResource(PackType type, ResourceLocation location) {
            return type == PackType.CLIENT_RESOURCES && shown(location) ? super.getResource(type, location) : null;
        }

        @Override
        public void listResources(PackType type, String namespace, String path, ResourceOutput output) {
            if (type == PackType.CLIENT_RESOURCES && NAMESPACES.contains(namespace)) {
                super.listResources(type, namespace, path, (location, supplier) -> {
                    if (shown(location)) {
                        output.accept(location, supplier);
                    }
                });
            }
        }

        @Override
        public Set<String> getNamespaces(PackType type) {
            return type == PackType.CLIENT_RESOURCES ? NAMESPACES : Set.of();
        }

        private static boolean shown(ResourceLocation location) {
            BooleanSupplier station = STATES.get(location);
            if (station != null) {
                return station.getAsBoolean();
            }
            return HandsOnConfig.cookingPot() && DYED_NAMESPACE.equals(location.getNamespace())
                    && location.getPath().startsWith(DYED_MODELS);
        }
    }
}
