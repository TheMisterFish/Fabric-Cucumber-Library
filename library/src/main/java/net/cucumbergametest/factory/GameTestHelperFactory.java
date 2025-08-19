package net.cucumbergametest.factory;

import io.cucumber.java.Scenario;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Rotation;
import org.jetbrains.annotations.NotNull;

public class GameTestHelperFactory {

    public static GameTestHelper create(Scenario scenario, MinecraftServer server) {
        if (server == null) {
            throw new IllegalStateException("MinecraftServer is not initialized.");
        }

        ServerLevel level = server.overworld();

        String batchName     = "cucumber";
        String testName      = scenario.getName();
        GameTestInfo info = getGameTestInfo(batchName, testName, level);

        GameTestTicker.SINGLETON.add(info);
        return new GameTestHelper(info);
    }

    private static @NotNull GameTestInfo getGameTestInfo(String batchName, String testName, ServerLevel level) {
        String structureName = "empty";
        Rotation rotation    = Rotation.NONE;
        int maxTicks         = 100;
        long setupTicks      = 0;
        boolean required     = false;

        TestFunction function = new TestFunction(
                batchName,
                testName,
                structureName,
                rotation,
                maxTicks,
                setupTicks,
                required,
                helper -> {} // no-op
        );

        RetryOptions retry = new RetryOptions(1, true);
        return new GameTestInfo(function, rotation, level, retry);
    }
}