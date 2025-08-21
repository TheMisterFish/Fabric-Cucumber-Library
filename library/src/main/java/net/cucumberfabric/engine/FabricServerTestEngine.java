package net.cucumberfabric.engine;

import io.cucumber.junit.platform.engine.CucumberTestEngine;
import net.cucumberfabric.dto.ExecutionReplyDTO;
import net.cucumberfabric.dto.MessageWrapperDTO;
import net.cucumberfabric.dto.TestRequestPayloadDTO;
import net.cucumberfabric.dto.types.MessageType;
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

public class FabricServerTestEngine implements TestEngine {

    public static final String ID = "fabric-server";
    private final CucumberTestEngine delegate = new CucumberTestEngine();
    private final ServerSocketHandler server = new ServerSocketHandler();
    private ConfigurationParameters configurationParameters;

    @Override
    public String getId() {
        return ID;
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


        try {
            server.start();
            startServer();

            boolean runDone = false;

            List<String> uniqueIds = request.getRootTestDescriptor().getDescendants().stream()
                    .map(testDescriptor -> testDescriptor.getUniqueId().toString())
                    .map(s -> s.replace("engine:fabric-server", "engine:cucumber"))
                    .toList();

            Map<String, String> stringParams = configurationParameters
                    .keySet()
                    .stream()
                    .flatMap(key -> configurationParameters.get(key).stream()
                            .map(value -> Map.entry(key, value)))
                    .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

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
                    String correctedUniqueId = executionReplyDTO.getUniqueId().replace("engine:cucumber", "engine:fabric-server");

                    Optional<? extends TestDescriptor> testDescriptorOptional = Stream.concat(
                                    Stream.of(request.getRootTestDescriptor()),
                                    request.getRootTestDescriptor().getDescendants().stream()
                            )
                            .filter(descriptor -> descriptor.getUniqueId().toString().equals(correctedUniqueId))
                            .findFirst();

                    if (testDescriptorOptional.isPresent()) {
                        TestDescriptor testDescriptor = testDescriptorOptional.get();
                        EngineExecutionListener engineExecutionListener = request.getEngineExecutionListener();

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

    private void startServer() {
        try {
            ClassLoader currentLoader = Thread.currentThread().getContextClassLoader();
            // 1) prepare a sandbox run directory
            Path base = Paths.get("cucumber_run");
            Path run = base.resolve("run");
            Path mods = base.resolve("mods");
            Files.createDirectories(run);
            Files.createDirectories(mods);

            // 2) Fabric dev flags
            System.setProperty("fabric.development", "true");
            System.setProperty("fabric.log.level", "info");
            System.setProperty("fabric.modsFolder", mods.toAbsolutePath().toString());
            System.setProperty("custom.server.dir", base.toAbsolutePath().toString());
            System.setProperty("custom.server.eula.location", base.toAbsolutePath().toString());

            // 3) launch the server (this sets the context loader to the new Knot loader)
            Knot.launch(new String[]{
                    "--nogui",
                    "gameDir", run.toAbsolutePath().toString()
            }, EnvType.SERVER);

            Thread.currentThread().setContextClassLoader(currentLoader);
        } catch (IOException ioe) {
            throw new RuntimeException("Failed to prepare Fabric run directory", ioe);
        }
    }
}