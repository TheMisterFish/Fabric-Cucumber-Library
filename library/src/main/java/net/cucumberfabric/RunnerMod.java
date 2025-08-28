package net.cucumberfabric;

import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import net.cucumberfabric.dto.MessageWrapperDTO;
import net.cucumberfabric.dto.TestRequestPayloadDTO;
import net.cucumberfabric.dto.types.MessageType;
import net.cucumberfabric.listener.CucumberTestListener;
import net.cucumberfabric.socket.ClientSocketHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static net.minecraft.server.network.ServerConnectionListener.SERVER_EPOLL_EVENT_GROUP;
import static net.minecraft.server.network.ServerConnectionListener.SERVER_EVENT_GROUP;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectUniqueId;

public class RunnerMod implements DedicatedServerModInitializer, ClientModInitializer {
    private final ClientSocketHandler clientSocketHandler = new ClientSocketHandler();
    private final Logger LOGGER = LoggerFactory.getLogger(RunnerMod.class);
    private MinecraftServer minecraftServer;
    private Minecraft minecraft;
    private TestRequestPayloadDTO testRequestPayloadDTO;
    private EnvType currentEnvtype;
    private int replyCounter = 0;
    private boolean started;


    @Override
    public void onInitializeServer() {
        currentEnvtype = EnvType.SERVER;
        ServerTickEvents.START_SERVER_TICK.register(this::onTick);

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            try {
                this.minecraftServer = server;
                this.clientSocketHandler.connect();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            if (clientSocketHandler.isConnected()) {
                try {
                    clientSocketHandler.sendObject(new MessageWrapperDTO(MessageType.SERVER_STOPPED));
                    clientSocketHandler.close();

                    closeNetty();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    @Override
    public void onInitializeClient() {
        currentEnvtype = EnvType.CLIENT;
        ClientTickEvents.START_CLIENT_TICK.register(this::onTick);

        ClientLifecycleEvents.CLIENT_STARTED.register(minecraft -> {
            try {
                this.minecraft = minecraft;
                this.clientSocketHandler.connect();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(minecraft -> {
            if (clientSocketHandler.isConnected()) {
                try {
                    clientSocketHandler.sendObject(new MessageWrapperDTO(MessageType.CLIENT_STOPPING));
                    clientSocketHandler.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private void onTick(Object ignored) {
        try {
            clientSocketHandler.poll();
            if (clientSocketHandler.isConnected()) {
                if (clientSocketHandler.hasMessages()) {
                    Object message = clientSocketHandler.getNextMessage();

                    if (message instanceof TestRequestPayloadDTO payload) {
                        this.testRequestPayloadDTO = payload;
                    }
                }

                replyCounter++;
                if (replyCounter > 20) {
                    clientSocketHandler.sendObject(new MessageWrapperDTO(MessageType.WORKING));
                    replyCounter = 0;
                }

                if (!started && testRequestPayloadDTO != null) {
                    if (currentEnvtype == EnvType.CLIENT) {
                        if (minecraft.isGameLoadFinished() && minecraft.gui.getGuiTicks() > 80) {
                            started = true;
                            startCucumberLauncher(testRequestPayloadDTO);
                        }
                    } else if (currentEnvtype == EnvType.SERVER) {
                        if (minecraftServer.isReady()) {
                            started = true;
                            startCucumberLauncher(testRequestPayloadDTO);
                        }
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

    private void startCucumberLauncher(TestRequestPayloadDTO testRequestPayloadDTO) throws IOException, InterruptedException {
        LauncherDiscoveryRequestBuilder requestBuilder = LauncherDiscoveryRequestBuilder.request();

        if (!testRequestPayloadDTO.getStringParams().isEmpty()) {
            requestBuilder.configurationParameters(testRequestPayloadDTO.getStringParams());
        }

        testRequestPayloadDTO.getUniqueIds().forEach(uniqueId -> requestBuilder.selectors(
                selectUniqueId(uniqueId)
        ));

        LauncherDiscoveryRequest request = requestBuilder.build();

        Launcher launcher = LauncherFactory.create();

        CucumberTestListener cucumberTestListener = new CucumberTestListener(clientSocketHandler);
        launcher.registerTestExecutionListeners(cucumberTestListener);

        new Thread(() -> {
            launcher.execute(request);

            try {
                clientSocketHandler.sendObject(new MessageWrapperDTO(MessageType.DONE));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            if (minecraftServer != null) {
                minecraftServer.close();
            }
            if (minecraft != null) {
                minecraft.stop();
            }
        }, "cucumber-test-thread").start();
    }

    private void closeNetty() {
        try {
            NioEventLoopGroup nioGroup = SERVER_EVENT_GROUP.get();
            nioGroup.shutdownGracefully().sync();

            EpollEventLoopGroup epollGroup = SERVER_EPOLL_EVENT_GROUP.get();
            epollGroup.shutdownGracefully().sync();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
