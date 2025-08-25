package net.cucumberfabric;

import io.cucumber.junit.platform.engine.CucumberTestEngine;
import net.cucumberfabric.dto.ExecutionReplyDTO;
import net.cucumberfabric.dto.MessageWrapperDTO;
import net.cucumberfabric.dto.TestRequestPayloadDTO;
import net.cucumberfabric.dto.types.MessageType;
import net.cucumberfabric.exception.FabricEngineTimeOutException;
import net.cucumberfabric.options.Constants;
import net.cucumberfabric.socket.ServerSocketHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.impl.launch.knot.Knot;
import net.minecraft.server.dedicated.DedicatedServerProperties;
import org.junit.platform.engine.*;
import org.junit.platform.engine.reporting.FileEntry;
import org.junit.platform.engine.reporting.ReportEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

public class FabricTestEngine implements TestEngine {
    public static final String FABRIC_ENGINE_ID = "fabric-cucumber";

    private final Logger LOGGER = LoggerFactory.getLogger(FabricTestEngine.class);
    private final CucumberTestEngine delegate = new CucumberTestEngine();
    private final ServerSocketHandler server = new ServerSocketHandler();

    private ConfigurationParameters configurationParameters;
    private int timeoutCounter;

    @Override
    public String getId() {
        return FABRIC_ENGINE_ID;
    }

    @Override
    public TestDescriptor discover(EngineDiscoveryRequest request, UniqueId uniqueId) {
        TestDescriptor root = delegate.discover(request, uniqueId);
        if (!root.getChildren().isEmpty()) {
            configurationParameters = request.getConfigurationParameters();
        }
        return root;
    }

    @Override
    public void execute(ExecutionRequest request) {
        if (request.getRootTestDescriptor().getChildren().isEmpty())
            return;

        String engineToUse = Constants.getEngineToUse(configurationParameters);
        EngineExecutionListener engineExecutionListener = request.getEngineExecutionListener();

        try {
            server.start();
            startFabricKnot();

            boolean runDone = false;
            boolean gameStopped = false;

            List<String> uniqueIds = request.getRootTestDescriptor().getDescendants().stream()
                    .map(testDescriptor -> testDescriptor.getUniqueId().toString())
                    .map(s -> s.replace("engine:" + FABRIC_ENGINE_ID, "engine:" + engineToUse))
                    .toList();

            Map<String, String> stringParams = configurationParameters
                    .keySet()
                    .stream()
                    .flatMap(key -> configurationParameters.get(key).stream()
                            .map(value -> Map.entry(key, value)))
                    .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

            stringParams = Constants.mergeWithDefaults(stringParams);

            TestRequestPayloadDTO payload = new TestRequestPayloadDTO(uniqueIds, stringParams);

            while (!server.isConnected()) {
                server.poll();
                if (timeoutSleep(100)) {
                    engineExecutionListener.executionFinished(
                            request.getRootTestDescriptor(),
                            TestExecutionResult.failed(new FabricEngineTimeOutException("The ModRunner did not connect in time"))
                    );
                    return;
                }
            }
            timeoutCounter = 0;


            TimeUnit.MILLISECONDS.sleep(500);
            server.sendObject(payload);

            while (!runDone || !gameStopped) {
                while (!server.hasMessages()) {
                    server.poll();
                    if (timeoutSleep(10)) {
                        engineExecutionListener.executionFinished(
                                request.getRootTestDescriptor(),
                                TestExecutionResult.failed(new FabricEngineTimeOutException("FabricEngine did not receive a message in time"))
                        );
                        return;
                    }
                }
                timeoutCounter = 0;


                Object newMessage = server.getNextMessage();

                if (newMessage instanceof ExecutionReplyDTO executionReplyDTO) {
                    String correctedUniqueId = executionReplyDTO.getUniqueId().replace("engine:" + engineToUse, "engine:" + FABRIC_ENGINE_ID);

                    Optional<? extends TestDescriptor> testDescriptorOptional = Stream.concat(
                                    Stream.of(request.getRootTestDescriptor()),
                                    request.getRootTestDescriptor().getDescendants().stream()
                            )
                            .filter(descriptor -> descriptor.getUniqueId().toString().equals(correctedUniqueId))
                            .findFirst();

                    if (testDescriptorOptional.isPresent()) {
                        TestDescriptor testDescriptor = testDescriptorOptional.get();

                        dispatchExecutionReply(executionReplyDTO, engineExecutionListener, testDescriptor);
                    }
                } else if (newMessage instanceof MessageWrapperDTO messageWrapperDTO) {
                    if (messageWrapperDTO.getMessageType().equals(MessageType.DONE)) {
                        runDone = true;
                    } else if (messageWrapperDTO.getMessageType().equals(MessageType.SERVER_STOPPED)) {
                        gameStopped = true;
                    } else if (messageWrapperDTO.getMessageType().equals(MessageType.CLIENT_STOPPING)) {
                        gameStopped = true;
                    } else if (messageWrapperDTO.getMessageType().equals(MessageType.WORKING)) {
                        LOGGER.info("RunnerMod still working");
                    } else if (messageWrapperDTO.getMessageType().equals(MessageType.ERROR)) {
                        throw new RuntimeException("Exception received from Mod", messageWrapperDTO.getMessage());
                    }
                }
            }
            System.out.println("DONE?");
            server.close();
        } catch (InterruptedException | IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean timeoutSleep(int millis) throws InterruptedException {
        TimeUnit.MILLISECONDS.sleep(millis);
        timeoutCounter = timeoutCounter + millis;
        return timeoutCounter > (Constants.getEngineTimeout(configurationParameters) * 1000);
    }

    private void startFabricKnot() {
        try {
            EnvType envType = Constants.getEnvType(configurationParameters);
            ClassLoader currentLoader = Thread.currentThread().getContextClassLoader();
            // 1) prepare a sandbox run directory
            Path base = Paths.get(Constants.getRootRunDir(configurationParameters));
            Path run = envType == EnvType.SERVER ?
                    base.resolve(Constants.getServerRunDir(configurationParameters)) :
                    base.resolve(Constants.getClientRunDir(configurationParameters));

            Path mods = base.resolve("mods");
            Files.createDirectories(run);
            Files.createDirectories(mods);

            // 2) Fabric dev flags
            System.setProperty("fabric.development", "true");
            System.setProperty("fabric.log.level", "info");
            System.setProperty("fabric.modsFolder", mods.toAbsolutePath().toString());

            // 3) launch the server (this sets the context loader to the new Knot loader)
            if (envType == EnvType.SERVER) {
                System.setProperty("cucumberfabric.server-dir", run.toAbsolutePath().toString());
                System.setProperty("cucumberfabric.eula-location", run.toAbsolutePath().toString());
                createServerProperties(run);

                Knot.launch(new String[]{
                        "--nogui",
                }, envType);
            } else if (envType == EnvType.CLIENT) {
                new Thread(() -> {
                    Knot.launch(new String[]{
                            "gameDir", run.toAbsolutePath().toString()
                    }, envType);
                }, "fabric-client-launcher").start();
            } else {
                throw new RuntimeException("EnvType was not SERVER or CLIENT");
            }

            Thread.currentThread().setContextClassLoader(currentLoader);

        } catch (IOException ioe) {
            throw new RuntimeException("Failed to prepare Fabric run directory", ioe);
        }
    }

    private static void createServerProperties(Path runDir) {
        Path path = runDir.resolve("server.properties");
        Properties properties = new Properties();
        new DedicatedServerProperties(properties);

        //set properties here

        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            properties.store(writer, "Minecraft server properties");
        } catch (IOException ex) {
            throw new UncheckedIOException(String.format("Failed to store properties to file: %s", path), ex);
        }
    }

    private void dispatchExecutionReply(ExecutionReplyDTO executionReplyDTO, EngineExecutionListener engineExecutionListener, TestDescriptor testDescriptor) {
        switch (executionReplyDTO.getExecutionType()) {
            case DYNAMIC_TEST_REGISTERED -> engineExecutionListener.dynamicTestRegistered(testDescriptor);
            case EXECUTION_SKIPPED ->
                    engineExecutionListener.executionSkipped(testDescriptor, executionReplyDTO.getReason());
            case EXECUTION_STARTED -> engineExecutionListener.executionStarted(testDescriptor);
            case EXECUTION_FINISHED -> {
                if (executionReplyDTO.toTestExecutionResult().isPresent()) {
                    engineExecutionListener.executionFinished(testDescriptor, executionReplyDTO.toTestExecutionResult().get());
                } else {
                    throw new RuntimeException(String.format("Could not find TestExecutionResult for TestDescriptor with  %s", executionReplyDTO.getUniqueId()));
                }
            }
            case REPORTING_ENTRY_PUBLISHED -> {
                if (executionReplyDTO.keyValuePairs().isPresent()) {
                    ReportEntry reportEntry = ReportEntry.from(executionReplyDTO.keyValuePairs().get());
                    engineExecutionListener.reportingEntryPublished(testDescriptor, reportEntry);
                } else {
                    throw new RuntimeException(String.format("Could not find ReportEntry for TestDescriptor with  %s", executionReplyDTO.getUniqueId()));
                }
            }
            case FILE_ENTRY_PUBLISHED -> {
                if (executionReplyDTO.getFilePath().isPresent()) {
                    FileEntry fileEntry = FileEntry.from(executionReplyDTO.getFilePath().get(), executionReplyDTO.getMediaType());
                    engineExecutionListener.fileEntryPublished(testDescriptor, fileEntry);
                } else {
                    throw new RuntimeException(String.format("Could not find FileEntry for TestDescriptor with  %s", executionReplyDTO.getUniqueId()));
                }
            }
        }
    }
}