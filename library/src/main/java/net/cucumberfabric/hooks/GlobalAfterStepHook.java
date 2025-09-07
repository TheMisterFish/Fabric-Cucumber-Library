package net.cucumberfabric.hooks;

import io.cucumber.java.After;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;

public class GlobalAfterStepHook {
    @After
    public void afterScenario() {
        var mcInstance = Minecraft.getInstance();
        System.out.println("KAAS");
        if (mcInstance.isRunning()) {
            mcInstance.execute(() -> mcInstance.disconnect(new TitleScreen()));
        }
    }
}