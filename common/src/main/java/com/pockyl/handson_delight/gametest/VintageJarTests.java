package com.pockyl.handson_delight.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;

import com.pockyl.diegesis.api.state.SlotRef;
import com.pockyl.handson_delight.platform.Platform;
import com.pockyl.handson_delight.station.KegLogic;
import com.pockyl.handson_delight.station.KegStation;
import com.pockyl.handson_delight.station.KitchenKeg;

import java.util.List;

/**
 * Vintage Delight's fermenting jar at the station: its recipes are read with the bottle or bowl some take; what is dropped in is laid
 * out one ingredient a slot and the bowl put in its container slot; under the lid it ferments in its own time and what is done waits
 * to be taken; with the lid off it waits. Each test passes at once where Vintage Delight is not there.
 */
public final class VintageJarTests {
    /** The jar's own slots: six for food, its container, its first result. */
    private static final int CONTAINER = 6;
    private static final int RESULT = 7;
    private static final String PROGRESS = "fermenting_jar.progress";

    private VintageJarTests() {
    }

    @SuppressWarnings("removal")
    private static Item vintage(String path) {
        return BuiltInRegistries.ITEM.get(new ResourceLocation(KegStation.VINTAGE_MOD_ID, path));
    }

    @SuppressWarnings("removal")
    private static String recipe(String path) {
        return new ResourceLocation(KegStation.VINTAGE_MOD_ID, "fermenting/" + path).toString();
    }

    private record Setup(PotTests.TestStation station, KegLogic logic, KitchenKeg jar, BlockPos pos) {
    }

    /** A jar on stone, with the logic the station would make for it; null without Vintage Delight. */
    private static Setup jar(GameTestHelper helper) {
        if (!Platform.INSTANCE.isLoaded(KegStation.VINTAGE_MOD_ID)) {
            helper.succeed();
            return null;
        }
        BlockPos at = new BlockPos(1, 2, 1);
        helper.setBlock(at.below(), Blocks.STONE);
        helper.setBlock(at, KegStation.JAR.get());
        BlockPos pos = helper.absolutePos(at);
        PotTests.TestStation station = new PotTests.TestStation(helper.getLevel(), pos, KegStation.JAR_TYPE);
        KitchenKeg jar = KitchenKeg.of(helper.getLevel().getBlockEntity(pos));
        helper.assertTrue(jar != null, "Vintage Delight's fermenting jar is a kitchen keg");
        return new Setup(station, new KegLogic(station), jar, pos);
    }

    private static void lid(Setup setup, Player player, boolean off) {
        CompoundTag data = new CompoundTag();
        data.putBoolean(KegStation.OPEN, off);
        setup.logic.onCustomAction(setup.station, player, KegStation.ACTION_LID, data);
    }

    private static KitchenKeg.Fermenting find(List<KitchenKeg.Fermenting> recipes, String id) {
        return recipes.stream().filter(recipe -> recipe.id().toString().equals(id)).findFirst().orElse(null);
    }

    /** One of each thing {@code recipe} takes into {@code player}'s inventory, {@code sets} times: the first item of each ingredient. */
    private static void give(Player player, KitchenKeg.Fermenting recipe, int sets) {
        for (int set = 0; set < sets; set++) {
            for (Ingredient ingredient : recipe.ingredients()) {
                player.getInventory().add(ingredient.getItems()[0].copyWithCount(1));
            }
        }
    }

    /** Brings what the jar ferments to a few ticks before it is done, through its own saved data. */
    private static void nearlyDone(GameTestHelper helper, Setup setup, int total) {
        BlockEntity entity = helper.getLevel().getBlockEntity(setup.pos);
        CompoundTag tag = entity.saveWithoutMetadata();
        tag.putInt(PROGRESS, total - 4);
        entity.load(tag);
    }

    private static int progress(GameTestHelper helper, Setup setup) {
        return helper.getLevel().getBlockEntity(setup.pos).saveWithoutMetadata().getInt(PROGRESS);
    }

    /** Its recipes are read as it ferments them: five pickles of five cucumbers and salt; overnight oats take their bowl last. */
    public static void vintageJarReadsItsRecipes(GameTestHelper helper) {
        Setup setup = jar(helper);
        if (setup == null) {
            return;
        }
        List<KitchenKeg.Fermenting> recipes = setup.jar.handson$recipes();
        helper.assertTrue(recipes.size() >= 20, "every fermenting recipe, got " + recipes.size());
        KitchenKeg.Fermenting pickles = find(recipes, recipe("pickle_from_fermenting"));
        helper.assertTrue(pickles != null && pickles.ingredients().size() == 6 && pickles.resultItem().is(vintage("pickle"))
                && pickles.resultItem().getCount() == 5 && pickles.ticks() == 2400, "five pickles of six things in two minutes");
        ItemStack cucumber = pickles.ingredients().get(0).getItems()[0];
        ItemStack salt = pickles.ingredients().get(5).getItems()[0];
        helper.assertTrue(setup.jar.handson$makes(pickles, List.of(cucumber, cucumber, cucumber, cucumber, cucumber, salt))
                && !setup.jar.handson$makes(pickles, List.of(cucumber.copyWithCount(5), salt)),
                "five cucumbers and salt, one a slot; a stack in one slot is not five");
        KitchenKeg.Fermenting oats = find(recipes, recipe("overnight_oats_from_fermenting"));
        helper.assertTrue(oats != null && oats.ingredients().size() == 7 && oats.ingredients().get(6).test(new ItemStack(Items.BOWL)),
                "overnight oats take six things and, last, their bowl");
        helper.assertTrue(!setup.jar.handson$feelsWarmth() && setup.jar.handson$fluid() == Fluids.EMPTY
                && !setup.jar.handson$isVessel(new ItemStack(Items.GLASS_BOTTLE)) && setup.jar.handson$brine() != 0
                && setup.jar.handson$shownAmount() == setup.jar.handson$capacity(), "no warmth, no liquid but its brine; a bottle goes in");
        helper.succeed();
    }

    /**
     * Things go in only with the lid off. Bowls dropped in among the food go to the jar's container slot; a stack of cucumbers and salt
     * thrown in are laid out one cucumber a slot, as the jar reads its recipe.
     */
    public static void vintageJarLaysOutWhatIsThrownIn(GameTestHelper helper) {
        Setup setup = jar(helper);
        if (setup == null) {
            return;
        }
        Player player = helper.makeMockPlayer();
        KitchenKeg.Fermenting pickles = find(setup.jar.handson$recipes(), recipe("pickle_from_fermenting"));
        ItemStack cucumber = pickles.ingredients().get(0).getItems()[0];
        ItemStack salt = pickles.ingredients().get(5).getItems()[0];
        SlotRef first = SlotRef.of(KegStation.INGREDIENTS, 0);
        setup.logic.tick(setup.station);
        helper.assertFalse(setup.logic.mayPlace(setup.station, player, first, cucumber), "nothing goes in through the lid");
        lid(setup, player, true);
        helper.assertTrue(setup.logic.mayPlace(setup.station, player, first, cucumber)
                && setup.logic.mayPlace(setup.station, player, first, new ItemStack(Items.BOWL)), "cucumbers and a bowl go in with the lid off");
        setup.station.state().setItem(first, new ItemStack(Items.BOWL, 2));
        setup.station.state().setItem(SlotRef.of(KegStation.INGREDIENTS, 1), cucumber.copyWithCount(10));
        setup.station.state().setItem(SlotRef.of(KegStation.INGREDIENTS, 2), salt.copyWithCount(2));
        setup.logic.onSlotChanged(setup.station, first);
        setup.logic.tick(setup.station);
        helper.assertTrue(setup.jar.handson$getItem(CONTAINER).is(Items.BOWL) && setup.jar.handson$getItem(CONTAINER).getCount() == 2
                && !setup.jar.handson$getItem(0).is(Items.BOWL), "the bowls lie in the jar's container slot, got "
                + setup.jar.handson$getItem(CONTAINER));
        // With the bowls taken out again, the food makes pickles and is laid out for them.
        setup.station.state().setItem(SlotRef.of(KegStation.INGREDIENTS, CONTAINER), ItemStack.EMPTY);
        setup.logic.onSlotChanged(setup.station, first);
        setup.logic.tick(setup.station);
        int cucumbers = 0;
        int salts = 0;
        for (int slot = 0; slot < CONTAINER; slot++) {
            ItemStack stack = setup.jar.handson$getItem(slot);
            if (ItemStack.isSameItem(stack, cucumber)) {
                helper.assertTrue(stack.getCount() == 2, "two cucumbers a slot, got " + stack);
                cucumbers++;
            } else if (ItemStack.isSameItem(stack, salt)) {
                salts += stack.getCount();
            }
        }
        helper.assertTrue(cucumbers == 5 && salts == 2, "cucumbers over five slots and the salt in one, got " + cucumbers + " and " + salts);
        helper.succeed();
    }

    /**
     * Pickles picked in the book: the lid comes off, a set goes in from the inventory. With the lid off the jar waits; under the lid it
     * ferments, and the five pickles wait in the served slot, taken with the lid on.
     */
    public static void vintagePicklesFermentUnderTheLid(GameTestHelper helper) {
        Setup setup = jar(helper);
        if (setup == null) {
            return;
        }
        Player player = helper.makeMockPlayer();
        KitchenKeg.Fermenting pickles = find(setup.jar.handson$recipes(), recipe("pickle_from_fermenting"));
        // A cook is at it: a jar nobody is at gets its lid back.
        setup.station.users.add(player);
        give(player, pickles, 1);
        CompoundTag fill = new CompoundTag();
        fill.putString(KegStation.DISH, pickles.id().toString());
        setup.logic.onCustomAction(setup.station, player, KegStation.ACTION_FILL, fill);
        helper.assertTrue(KegLogic.isOpen(setup.station), "the book took the lid off");
        for (int slot = 0; slot < CONTAINER; slot++) {
            helper.assertTrue(setup.jar.handson$getItem(slot).getCount() == 1, "one thing in each of its six slots, slot " + slot);
        }
        helper.assertTrue(player.getInventory().isEmpty(), "the set came out of the inventory");
        int[] held = new int[1];
        helper.runAfterDelay(6, () -> {
            setup.logic.tick(setup.station);
            held[0] = progress(helper, setup);
        });
        helper.runAfterDelay(12, () -> {
            setup.logic.tick(setup.station);
            helper.assertTrue(progress(helper, setup) <= held[0] + 1, "with the lid off it waits, " + held[0] + " then "
                    + progress(helper, setup));
            lid(setup, player, false);
            setup.logic.tick(setup.station);
        });
        helper.runAfterDelay(20, () -> {
            setup.logic.tick(setup.station);
            helper.assertTrue(setup.jar.handson$data().get(0) > 4 && setup.jar.handson$data().get(1) == 2400,
                    "it ferments under the lid, " + setup.jar.handson$data().get(0) + " of " + setup.jar.handson$data().get(1));
            helper.assertTrue(setup.station.state().getControl(KegStation.PROGRESS) > 0.0F, "the station shows it");
            nearlyDone(helper, setup, 2400);
        });
        helper.runAfterDelay(32, () -> {
            setup.logic.tick(setup.station);
            ItemStack served = setup.station.state().getItem(KegStation.SERVED_SLOT);
            helper.assertTrue(served.is(vintage("pickle")) && served.getCount() == 5, "five pickles wait, got " + served);
            helper.assertFalse(KegLogic.isOpen(setup.station), "the lid still on");
            helper.assertTrue(setup.logic.mayTake(setup.station, player, KegStation.SERVED_SLOT), "taken with the lid on");
            setup.station.state().setItem(KegStation.SERVED_SLOT, ItemStack.EMPTY);
            setup.logic.onTaken(setup.station, player, KegStation.SERVED_SLOT, served);
            helper.assertTrue(setup.jar.handson$getItem(RESULT).isEmpty(), "taken out of the jar's result slot");
            for (int slot = 0; slot < CONTAINER; slot++) {
                helper.assertTrue(setup.jar.handson$getItem(slot).isEmpty(), "the set is used up, slot " + slot);
            }
            helper.succeed();
        });
    }

    /**
     * Overnight oats from the book, two sets: their bowls go to the jar's container slot with the food laid out, and the jar takes one
     * with each bowl of oats it makes.
     */
    public static void vintageOatsTakeTheirBowl(GameTestHelper helper) {
        Setup setup = jar(helper);
        if (setup == null) {
            return;
        }
        Player player = helper.makeMockPlayer();
        KitchenKeg.Fermenting oats = find(setup.jar.handson$recipes(), recipe("overnight_oats_from_fermenting"));
        give(player, oats, 2);
        CompoundTag fill = new CompoundTag();
        fill.putString(KegStation.DISH, oats.id().toString());
        fill.putBoolean(KegStation.ALL, true);
        setup.logic.onCustomAction(setup.station, player, KegStation.ACTION_FILL, fill);
        helper.assertTrue(setup.jar.handson$getItem(CONTAINER).is(Items.BOWL) && setup.jar.handson$getItem(CONTAINER).getCount() == 2,
                "two bowls in the jar's container slot, got " + setup.jar.handson$getItem(CONTAINER));
        int things = 0;
        for (int slot = 0; slot < CONTAINER; slot++) {
            helper.assertFalse(setup.jar.handson$getItem(slot).isEmpty() || setup.jar.handson$getItem(slot).is(Items.BOWL),
                    "food in each of its six slots, got " + setup.jar.handson$getItem(slot));
            things += setup.jar.handson$getItem(slot).getCount();
        }
        helper.assertTrue(things == 12, "two sets of six things, got " + things);
        lid(setup, player, false);
        helper.runAfterDelay(6, () -> {
            setup.logic.tick(setup.station);
            helper.assertTrue(setup.jar.handson$data().get(0) > 0 && setup.jar.handson$data().get(1) == oats.ticks(),
                    "the jar ferments the oats, " + setup.jar.handson$data().get(0) + " of " + setup.jar.handson$data().get(1));
            nearlyDone(helper, setup, oats.ticks());
        });
        helper.runAfterDelay(18, () -> {
            setup.logic.tick(setup.station);
            ItemStack served = setup.station.state().getItem(KegStation.SERVED_SLOT);
            helper.assertTrue(served.is(vintage("overnight_oats")) && served.getCount() == 1, "a bowl of oats waits, got " + served);
            helper.assertTrue(setup.jar.handson$getItem(CONTAINER).getCount() == 1, "one bowl left for the next");
            helper.succeed();
        });
    }
}
