package com.pockyl.handson_delight.forge;

import net.minecraftforge.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import com.pockyl.handson_delight.HandsOnDelight;
import com.pockyl.handson_delight.compat.OptionalMixins;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Applies the mixins into addons' own blocks only where they fit. The addon must be there under its own mod id: Youkai's Homecoming
 * ships classes of the same names as Youkai's Feasts, older and different. And its version must have what the mixin reaches into:
 * early Youkai's Feasts and Chopper's Delight lack members the stations use. A mixin that does not fit stops the game at start, or the
 * station at its first use; left out, the addon's block keeps its own use. Asked before any class loads, from the bytecode alone.
 */
public final class AddonMixins implements IMixinConfigPlugin {
    private static final String PACKAGE = "com.pockyl.handson_delight.forge.mixin.";
    private static final String YOUKAI = "youkaisfeasts";
    private static final String CHOPPERS = "choppersdelight";
    /** Youkai's Feasts' drinks as fluids, which its kettle and fermentation tank stations pour and tap: from 0.4.5 on. */
    private static final String YOUKAI_DRINKS = "dev.xkmc.youkaishomecoming.content.item.fluid.IYHFluidItem";

    /** What a mixin needs besides its target: the mod, and a member of the target or another class of the mod (null for none). */
    private record Needs(String modId, String member, String otherClass) {
        static Needs mod(String modId) {
            return new Needs(modId, null, null);
        }
    }

    /** The mixins into addons whose versions differ. The others (Miner's Delight, Dungeons Delight, ...) need their target only. */
    private static final Map<String, Needs> NEEDS = Map.ofEntries(
            Map.entry("YoukaiKettleMixin", new Needs(YOUKAI, null, YOUKAI_DRINKS)),
            Map.entry("YoukaiTankMixin", new Needs(YOUKAI, null, YOUKAI_DRINKS)),
            Map.entry("YoukaiPotMixin", new Needs(YOUKAI, "getSoup", null)),
            Map.entry("YoukaiTimedAccess", Needs.mod(YOUKAI)),
            Map.entry("YoukaiKettleRecipeAccess", Needs.mod(YOUKAI)),
            Map.entry("YoukaiFermentRecipeAccess", Needs.mod(YOUKAI)),
            Map.entry("YoukaiCuisineBoardMixin", Needs.mod(YOUKAI)),
            Map.entry("YoukaiSteamerMixin", Needs.mod(YOUKAI)),
            Map.entry("YoukaiPotSoupMixin", Needs.mod(YOUKAI)),
            Map.entry("YoukaiHintsMixin", Needs.mod(YOUKAI)),
            Map.entry("ChoppersBoardMixin", new Needs(CHOPPERS, "getMaxStackSize", null)),
            Map.entry("ChoppersBoardBlockMixin", Needs.mod(CHOPPERS)),
            Map.entry("DecoratedBoardContentMixin", Needs.mod(CHOPPERS)));

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        Needs needs = NEEDS.get(mixinClassName.substring(PACKAGE.length()));
        if (needs != null && FMLLoader.getLoadingModList().getModFileById(needs.modId()) == null) {
            return false;
        }
        ClassNode target = OptionalMixins.classNode(targetClassName);
        if (target == null) {
            return false;
        }
        if (needs != null && (needs.member() != null && !declares(target, needs.member())
                || needs.otherClass() != null && OptionalMixins.classNode(needs.otherClass()) == null)) {
            HandsOnDelight.LOGGER.warn("This version of {} is older than its station needs: {} is left out", needs.modId(),
                    mixinClassName.substring(PACKAGE.length()));
            return false;
        }
        return true;
    }

    private static boolean declares(ClassNode node, String member) {
        return node.methods.stream().anyMatch(method -> method.name.equals(member))
                || node.fields.stream().anyMatch(field -> field.name.equals(member));
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
