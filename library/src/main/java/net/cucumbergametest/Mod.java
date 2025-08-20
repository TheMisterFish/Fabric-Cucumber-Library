package net.cucumbergametest;

import net.cucumbergametest.dto.MessageType;
import net.cucumbergametest.dto.MessageWrapper;
import net.cucumbergametest.dto.TestRequestPayload;
import net.cucumbergametest.engine.FabricClientTestEngine;
import net.cucumbergametest.engine.FabricServerTestEngine;
import net.cucumbergametest.listener.ModCucumberTestListener;
import net.cucumbergametest.socket.SocketClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectUniqueId;
import static org.junit.platform.launcher.EngineFilter.excludeEngines;
import static org.junit.platform.launcher.EngineFilter.includeEngines;

public class Mod implements ModInitializer {
    private final SocketClient socketClient = new SocketClient();
    private final Logger LOGGER = LoggerFactory.getLogger(Mod.class);

    @Override
    public void onInitialize() {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) {
            ServerTickEvents.START_SERVER_TICK.register(this::onServerTick);

        } else {
            ClientTickEvents.START_CLIENT_TICK.register(this::onClientTick);
        }

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            try {
                socketClient.connect();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    private void onClientTick(Minecraft minecraft) {
        onServerTick();
    }

    private void onServerTick(MinecraftServer minecraftServer) {
        onServerTick();
    }

    private void onServerTick() {
        try {
            socketClient.poll();
            if (socketClient.isConnected()) {
                if (socketClient.hasMessages()) {
                    Object message = socketClient.getNextMessage();

                    if (message instanceof TestRequestPayload testRequestPayload) {
                        startCucumberLauncher(testRequestPayload);
                    }
                }
            }
        } catch (Exception e) {
            try {
                socketClient.sendObject(new MessageWrapper(MessageType.ERROR, e));
            } catch (IOException ioException) {
                throw new RuntimeException(ioException);
            }
            throw new RuntimeException(e);
        }
    }

    private void startCucumberLauncher(TestRequestPayload testRequestPayload) throws IOException {
        LauncherDiscoveryRequestBuilder requestBuilder = LauncherDiscoveryRequestBuilder.request();

        if (!testRequestPayload.getStringParams().isEmpty()) {
            requestBuilder.configurationParameters(testRequestPayload.getStringParams());
        }

        testRequestPayload.getUniqueIds().forEach(uniqueId -> {
            requestBuilder.selectors(
                    selectUniqueId(uniqueId)
            );
        });

        requestBuilder.configurationParameter("cucumber.glue", "steps")
        .filters(
                includeEngines("cucumber"),
                excludeEngines(FabricServerTestEngine.ID)
        );

        LauncherDiscoveryRequest request = requestBuilder.build();

        Launcher launcher = LauncherFactory.create();

        ModCucumberTestListener modCucumberTestListener = new ModCucumberTestListener(socketClient);
        launcher.registerTestExecutionListeners(modCucumberTestListener);

        launcher.discover(request);
        launcher.execute(request);

        socketClient.sendObject(new MessageWrapper(MessageType.DONE));
    }
}
