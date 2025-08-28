package net.cucumberfabric.helper.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class GuiHelper {
    public static List<String> listButtons() {
        List<String> buttons = new ArrayList<>();
        Minecraft.getInstance().execute(() -> {
            Screen currentScreen = Minecraft.getInstance().screen;
            if (currentScreen == null)
                return;
            for (GuiEventListener widget : currentScreen.children()) {
                if (widget instanceof Button foundButton) {
                    buttons.add(foundButton.getMessage().toString());
                }
            }
        });
        return buttons;
    }

    public static void pressButton(String button) {
        Minecraft.getInstance().execute(() -> {
            Screen currentScreen = Minecraft.getInstance().screen;
            if (currentScreen == null)
                return;
            for (GuiEventListener widget : currentScreen.children()) {
                if (widget instanceof Button foundButton) {
                    if (foundButton.getMessage().toString().contains(button)) {
                        foundButton.onPress();
                    }
                }
            }
        });
        sleep(200);
    }

    private static void sleep(int millis) {
        try {
            TimeUnit.MILLISECONDS.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
