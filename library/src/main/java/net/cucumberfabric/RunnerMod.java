package net.cucumberfabric;

import net.cucumberfabric.dto.types.MessageType;
import net.cucumberfabric.dto.MessageWrapperDTO;
import net.cucumberfabric.dto.TestRequestPayloadDTO;
import net.cucumberfabric.listener.CucumberTestListener;
import net.cucumberfabric.socket.ClientSocketHandler;
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

public class RunnerMod implements ModInitializer {
    private final ClientSocketHandler clientSocketHandler = new ClientSocketHandler();
    private final Logger LOGGER = LoggerFactory.getLogger(RunnerMod.class);

    @Override
    public void onInitialize() {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) {
            ServerTickEvents.START_SERVER_TICK.register(this::onServerTick);

        } else {
            ClientTickEvents.START_CLIENT_TICK.register(this::onClientTick);
        }

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            try {
                clientSocketHandler.connect();
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
            clientSocketHandler.poll();
            if (clientSocketHandler.isConnected()) {
                if (clientSocketHandler.hasMessages()) {
                    Object message = clientSocketHandler.getNextMessage();

                    if (message instanceof TestRequestPayloadDTO testRequestPayloadDTO) {
                        startCucumberLauncher(testRequestPayloadDTO);
                    }
                }
            }
        } catch (Exception e) {
            try {
                clientSocketHandler.sendObject(new MessageWrapperDTO(MessageType.ERROR, e));
            } catch (IOException ioException) {
                throw new RuntimeException(ioException);
            }
            throw new RuntimeException(e);
        }
    }

    private void startCucumberLauncher(TestRequestPayloadDTO testRequestPayloadDTO) throws IOException {
        LauncherDiscoveryRequestBuilder requestBuilder = LauncherDiscoveryRequestBuilder.request();

        if (!testRequestPayloadDTO.getStringParams().isEmpty()) {
            requestBuilder.configurationParameters(testRequestPayloadDTO.getStringParams());
        }

        testRequestPayloadDTO.getUniqueIds().forEach(uniqueId -> {
            requestBuilder.selectors(
                    selectUniqueId(uniqueId)
            );
        });

        LauncherDiscoveryRequest request = requestBuilder.build();

        Launcher launcher = LauncherFactory.create();

        CucumberTestListener cucumberTestListener = new CucumberTestListener(clientSocketHandler);
        launcher.registerTestExecutionListeners(cucumberTestListener);

        launcher.execute(request);

        clientSocketHandler.sendObject(new MessageWrapperDTO(MessageType.DONE));
    }
}
