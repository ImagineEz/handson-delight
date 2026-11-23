package com.pockyl.freeform_delight.forge;

import com.pockyl.diegesis.api.client.StationView;
import com.pockyl.diegesis.impl.client.focus.FocusScreen;
import com.pockyl.freeform_delight.client.PotBehavior;
import com.pockyl.freeform_delight.station.CookingPotStation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ScreenEvent;

import java.util.Arrays;

/** Draws the Cook button over the existing focused scene and handles only clicks on that button. */
final class FreeformCookButton {
    private static final int WIDTH = 74;
    private static final int HEIGHT = 20;
    private static final int TOP = 12;
    private static final int MARGIN = 12;
    private static final Component COOK = Component.literal("Cook");

    private FreeformCookButton() {
    }

    static void render(ScreenEvent.Render.Post event) {
        StationView view = activeView(event.getScreen());
        if (view == null) {
            return;
        }
        int x = left(event.getScreen());
        int end = x + WIDTH;
        boolean enabled = hasIngredients(view);
        boolean hover = enabled && isOver(event.getScreen(), event.getMouseX(), event.getMouseY());
        GuiGraphics graphics = event.getGuiGraphics();
        graphics.fill(x, TOP, end, TOP + HEIGHT, 0xFF181D18);
        graphics.fill(x + 1, TOP + 1, end - 1, TOP + HEIGHT - 1,
                enabled ? (hover ? 0xFF599043 : 0xFF467734) : 0xFF555A55);
        graphics.fill(x + 1, TOP + 1, end - 1, TOP + 2, enabled ? 0xFF98BB7D : 0xFF777C77);
        graphics.fill(x + 1, TOP + HEIGHT - 3, end - 1, TOP + HEIGHT - 1, enabled ? 0xFF2B5020 : 0xFF383C38);
        if (view.state().data().getBoolean(CookingPotStation.FREEFORM_ACTIVE)) {
            int progress = Math.round((WIDTH - 2) * view.state().getControl(CookingPotStation.PROGRESS));
            graphics.fill(x + 1, TOP + HEIGHT - 3, x + 1 + progress, TOP + HEIGHT - 1, 0xFFD5EAAD);
        }
        graphics.drawCenteredString(Minecraft.getInstance().font, COOK, x + WIDTH / 2, TOP + 6,
                enabled ? 0xFFFFFF : 0xB8BCB8);
    }

    static void click(ScreenEvent.MouseButtonPressed.Pre event) {
        StationView view = activeView(event.getScreen());
        if (view == null || event.getButton() != 0 || !isOver(event.getScreen(), event.getMouseX(), event.getMouseY())) {
            return;
        }
        event.setCanceled(true);
        if (hasIngredients(view)) {
            view.sendAction(CookingPotStation.ACTION_COOK, new CompoundTag());
        }
    }

    private static StationView activeView(Screen screen) {
        return screen instanceof FocusScreen ? PotBehavior.freeformFocus() : null;
    }

    private static boolean hasIngredients(StationView view) {
        return Arrays.stream(view.state().getItems(CookingPotStation.INGREDIENTS)).anyMatch(stack -> !stack.isEmpty());
    }

    private static int left(Screen screen) {
        return screen.width - WIDTH - MARGIN;
    }

    private static boolean isOver(Screen screen, double x, double y) {
        int left = left(screen);
        return x >= left && x < left + WIDTH && y >= TOP && y < TOP + HEIGHT;
    }
}
