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
 * Applies the mixins into addons' own blocks only where they fit. The addon must be there under its own mod id. And its version must
 * have what the mixin reaches into: early builds of the Youkai mods and Chopper's Delight lack members the stations use. A mixin that
 * does not fit stops the game at start, or the station at its first use; left out, the addon's block keeps its own use. Asked before
 * any class loads, from the bytecode alone.
 * <p>
 * Youkai's Feasts and Youkai's Homecoming are one code base under two mod ids (Feasts is Homecoming's food on its own, never installed
 * beside it): the same classes, so the same mixins, and the same members mark the same builds. Feasts 0.4.5 is Homecoming 2.6.6, the
 * first build of each with every member the stations use; Homecoming before 2.5.0 (Feasts began with 2.5.0's code) has other classes
 * of the same names.
 */
public final class AddonMixins implements IMixinConfigPlugin {
    private static final String PACKAGE = "com.pockyl.handson_delight.forge.mixin.";
    private static final List<String> YOUKAI = List.of("youkaisfeasts", "youkaishomecoming");
    private static final List<String> CHOPPERS = List.of("choppersdelight");
    private static final List<String> HEARTH = List.of("hearthandharvest");
    /** The Youkai mods' drinks as fluids, which their kettle and tank stations pour and tap: Feasts 0.4.5, Homecoming 2.6.6. */
    private static final String YOUKAI_DRINKS = "dev.xkmc.youkaishomecoming.content.item.fluid.IYHFluidItem";
    /** The Youkai mods' slip bottle as a container of a thing, which their pots and cuisine board give back: Homecoming 2.5.4 on. */
    private static final String YOUKAI_SLIP_BOTTLE = "dev.xkmc.youkaishomecoming.content.item.fluid.SlipBottleItem";
    private static final String YOUKAI_SLIP_DRAINED = "isSlipContainer";
    /** Steamer contents of a whole rack, which the steamer station lays: Homecoming 2.5.0 on, when its racks took things by place. */
    private static final String YOUKAI_STEAMER_CONTENT = "dev.xkmc.youkaishomecoming.content.block.food.ISteamerContentBlock";

    /**
     * What a mixin needs besides its target: one of the mods, a member of the target (null for none), and another class of the mod
     * (null for none) with a member of its own (null for any).
     */
    private record Needs(List<String> modIds, String member, String otherClass, String otherMember) {
        static Needs mod(List<String> modIds) {
            return new Needs(modIds, null, null, null);
        }

        static Needs member(List<String> modIds, String member) {
            return new Needs(modIds, member, null, null);
        }

        static Needs other(List<String> modIds, String otherClass, String otherMember) {
            return new Needs(modIds, null, otherClass, otherMember);
        }
    }

    /** The mixins into addons whose versions differ. The others (Miner's Delight, Dungeons Delight, ...) need their target only. */
    private static final Map<String, Needs> NEEDS = Map.ofEntries(
            Map.entry("YoukaiKettleMixin", Needs.other(YOUKAI, YOUKAI_DRINKS, null)),
            Map.entry("YoukaiKettleRecipeAccess", Needs.other(YOUKAI, YOUKAI_DRINKS, null)),
            Map.entry("YoukaiTankMixin", Needs.other(YOUKAI, YOUKAI_DRINKS, null)),
            Map.entry("YoukaiFermentRecipeAccess", Needs.other(YOUKAI, YOUKAI_DRINKS, null)),
            Map.entry("YoukaiPotMixin", new Needs(YOUKAI, "getSoup", YOUKAI_SLIP_BOTTLE, YOUKAI_SLIP_DRAINED)),
            Map.entry("YoukaiCuisineBoardMixin", Needs.other(YOUKAI, YOUKAI_SLIP_BOTTLE, YOUKAI_SLIP_DRAINED)),
            Map.entry("YoukaiSteamerMixin", Needs.other(YOUKAI, YOUKAI_STEAMER_CONTENT, null)),
            Map.entry("YoukaiTimedAccess", Needs.mod(YOUKAI)),
            Map.entry("YoukaiPotSoupMixin", Needs.mod(YOUKAI)),
            Map.entry("YoukaiHintsMixin", Needs.mod(YOUKAI)),
            Map.entry("ChoppersBoardMixin", Needs.member(CHOPPERS, "getMaxStackSize")),
            Map.entry("ChoppersBoardBlockMixin", Needs.mod(CHOPPERS)),
            Map.entry("DecoratedBoardContentMixin", Needs.mod(CHOPPERS)),
            Map.entry("CulinaryBridgesSyncMixin", Needs.mod(List.of("culinarycompat"))),
            // Hearth and Harvest has had its keg, with its two tanks, since 1.4.0.
            Map.entry("HearthKegMixin", Needs.member(HEARTH, "getInputTank")),
            Map.entry("HearthKegAccess", Needs.member(HEARTH, "getInputTank")),
            // Its cask was sealed by hand, and aged only so, since 1.4.0.
            Map.entry("HearthCaskMixin", Needs.member(HEARTH, "isSealed")),
            Map.entry("HearthCaskAccess", Needs.member(HEARTH, "isSealed")));

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
        String modId = needs == null ? null : needs.modIds().stream()
                .filter(id -> FMLLoader.getLoadingModList().getModFileById(id) != null).findFirst().orElse(null);
        if (needs != null && modId == null) {
            return false;
        }
        ClassNode target = OptionalMixins.classNode(targetClassName);
        if (target == null) {
            return false;
        }
        if (needs != null && (needs.member() != null && !declares(target, needs.member()) || needs.otherClass() != null
                && !has(OptionalMixins.classNode(needs.otherClass()), needs.otherMember()))) {
            HandsOnDelight.LOGGER.warn("This version of {} is older than its station needs: {} is left out", modId,
                    mixinClassName.substring(PACKAGE.length()));
            return false;
        }
        return true;
    }

    private static boolean has(ClassNode node, String member) {
        return node != null && (member == null || declares(node, member));
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
