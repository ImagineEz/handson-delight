package com.pockyl.handson_delight.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import com.pockyl.handson_delight.HandsOnDelight;

/** {@link RecipeEditTests} on Forge. */
@GameTestHolder(HandsOnDelight.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ForgeRecipeEditTests {
    private ForgeRecipeEditTests() {
    }

    @GameTest(template = "empty")
    public static void everyDishIsTheServersRecipe(GameTestHelper helper) {
        RecipeEditTests.everyDishIsTheServersRecipe(helper);
    }

    @GameTest(template = "empty")
    public static void aScriptsEditsReachTheBoard(GameTestHelper helper) {
        RecipeEditTests.aScriptsEditsReachTheBoard(helper);
    }
}
