package net.cucumberfabric.engine;

import io.cucumber.junit.platform.engine.CucumberTestEngine;
import net.cucumberfabric.dto.ExecutionReplyDTO;
import net.cucumberfabric.dto.MessageWrapperDTO;
import net.cucumberfabric.dto.TestRequestPayloadDTO;
import net.cucumberfabric.dto.types.MessageType;
import net.cucumberfabric.options.Constants;
import net.cucumberfabric.socket.ServerSocketHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.impl.launch.knot.Knot;
import org.junit.platform.engine.*;
import org.junit.platform.engine.reporting.FileEntry;
import org.junit.platform.engine.reporting.ReportEntry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

public class FabricTestEngine implements TestEngine {

    public static final String FABRIC_ENGINE_ID = "fabric-cucumber";
    private final CucumberTestEngine delegate = new CucumberTestEngine();
    private final ServerSocketHandler server = new ServerSocketHandler();
    private ConfigurationParameters configurationParameters;

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

        String engineToUse = Constants.getEngineToUse();

        try {
            server.start();
            startFabricKnot(Constants.getEnvType(), Constants.getRunDir());

            boolean runDone = false;

            List<String> uniqueIds = request.getRootTestDescriptor().getDescendants().stream()
                    .map(testDescriptor -> testDescriptor.getUniqueId().toString())
                    .map(s -> s.replace("engine:fabric-server", "engine:" + engineToUse))
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
                TimeUnit.MILLISECONDS.sleep(100);
            }


            TimeUnit.MILLISECONDS.sleep(500);
            server.sendObject(payload);

            while (!runDone) {
                while (!server.hasMessages()) {
                    server.poll();
                    TimeUnit.MILLISECONDS.sleep(10);
                }
                Object newMessage = server.getNextMessage();

                if (newMessage instanceof ExecutionReplyDTO executionReplyDTO) {
                    String correctedUniqueId = executionReplyDTO.getUniqueId().replace("engine:" + engineToUse, "engine:fabric-server");

                    Optional<? extends TestDescriptor> testDescriptorOptional = Stream.concat(
                                    Stream.of(request.getRootTestDescriptor()),
                                    request.getRootTestDescriptor().getDescendants().stream()
                            )
                            .filter(descriptor -> descriptor.getUniqueId().toString().equals(correctedUniqueId))
                            .findFirst();

                    if (testDescriptorOptional.isPresent()) {
                        TestDescriptor testDescriptor = testDescriptorOptional.get();
                        EngineExecutionListener engineExecutionListener = request.getEngineExecutionListener();

                        dispatchExecutionReply(executionReplyDTO, engineExecutionListener, testDescriptor);
                    }
                } else if (newMessage instanceof MessageWrapperDTO messageWrapperDTO) {
                    if (messageWrapperDTO.getMessageType().equals(MessageType.DONE)) {
                        runDone = true;
                    } else if (messageWrapperDTO.getMessageType().equals(MessageType.ERROR)) {
                        throw new RuntimeException("Exception received from Mod", messageWrapperDTO.getMessage());
                    }
                }
            }


        } catch (InterruptedException | IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void startFabricKnot(EnvType envType, String runDirectory) {
        try {
            ClassLoader currentLoader = Thread.currentThread().getContextClassLoader();
            // 1) prepare a sandbox run directory
            Path base = Paths.get(Constants.getRootRunDir());
            Path run = base.resolve(runDirectory);
            Path mods = base.resolve("mods");
            Files.createDirectories(run);
            Files.createDirectories(mods);

            // 2) Fabric dev flags
            System.setProperty("fabric.development", "true");
            System.setProperty("fabric.log.level", "info");
            System.setProperty("fabric.modsFolder", mods.toAbsolutePath().toString());

            // 3) launch the server (this sets the context loader to the new Knot loader)
            if(envType == EnvType.SERVER){
                System.setProperty("cucumberfabric.server-dir", base.toAbsolutePath().toString());
                System.setProperty("cucumberfabric.eula-location", base.toAbsolutePath().toString());

                Knot.launch(new String[]{
                        "--nogui",
                }, envType);
            } else {
                Knot.launch(new String[]{
                        "gameDir", run.toAbsolutePath().toString()
                }, envType);
            }

            Thread.currentThread().setContextClassLoader(currentLoader);

        } catch (IOException ioe) {
            throw new RuntimeException("Failed to prepare Fabric run directory", ioe);
        }
    }

    private void dispatchExecutionReply(ExecutionReplyDTO executionReplyDTO, EngineExecutionListener engineExecutionListener, TestDescriptor testDescriptor) {
        switch (executionReplyDTO.getExecutionType()) {
            case DYNAMIC_TEST_REGISTERED ->
                    engineExecutionListener.dynamicTestRegistered(testDescriptor);
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