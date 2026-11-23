package com.pockyl.freeform_delight;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import com.pockyl.freeform_delight.station.CookingPotStation;
import com.pockyl.freeform_delight.station.CuttingBoardStation;
import com.pockyl.freeform_delight.station.KegStation;
import com.pockyl.freeform_delight.station.BasinStation;
import com.pockyl.freeform_delight.station.CuisineBoardStation;
import com.pockyl.freeform_delight.station.GrillStation;
import com.pockyl.freeform_delight.station.KettleStation;
import com.pockyl.freeform_delight.station.OvenStation;
import com.pockyl.freeform_delight.station.SkilletStation;
import com.pockyl.freeform_delight.station.StoveStation;
import com.pockyl.freeform_delight.station.SteamerStation;
import com.pockyl.freeform_delight.station.YoukaiPotStation;

/** Turns the menus of Farmer's Delight into stations of Diegesis. The mod registers nothing: blocks and items stay theirs. */
public final class HandsOnDelight {
    public static final String MOD_ID = "freeform_delight";
    public static final Logger LOGGER = LogUtils.getLogger();

    private HandsOnDelight() {
    }

    /** Called by each loader's entry point on both sides. */
    public static void init() {
        HandsOnConfig.load();
        if (HandsOnConfig.cookingPot()) {
            CookingPotStation.register();
        }
        if (HandsOnConfig.cuttingBoard()) {
            CuttingBoardStation.register();
        }
        if (HandsOnConfig.skillet()) {
            SkilletStation.register();
        }
        if (HandsOnConfig.stove()) {
            StoveStation.register();
        }
        if (HandsOnConfig.keg()) {
            KegStation.register();
        }
        if (HandsOnConfig.youkaiTank()) {
            KegStation.registerTank();
        }
        if (HandsOnConfig.cask()) {
            KegStation.registerCask();
        }
        if (HandsOnConfig.kettle()) {
            KettleStation.register();
        }
        if (HandsOnConfig.grill()) {
            GrillStation.register();
        }
        if (HandsOnConfig.basin()) {
            BasinStation.register();
        }
        if (HandsOnConfig.cuisineBoard()) {
            CuisineBoardStation.register();
        }
        if (HandsOnConfig.youkaiPots()) {
            YoukaiPotStation.register();
        }
        if (HandsOnConfig.youkaiSteamer()) {
            SteamerStation.register();
        }
        if (HandsOnConfig.oven()) {
            OvenStation.register();
        }
    }

    @SuppressWarnings("removal")
    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
