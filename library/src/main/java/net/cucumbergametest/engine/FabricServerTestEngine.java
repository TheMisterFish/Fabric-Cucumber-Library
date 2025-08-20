package net.cucumbergametest.engine;

import io.cucumber.junit.platform.engine.CucumberTestEngine;
import net.cucumbergametest.dto.ExecutionReply;
import net.cucumbergametest.dto.MessageType;
import net.cucumbergametest.dto.MessageWrapper;
import net.cucumbergametest.dto.TestRequestPayload;
import net.cucumbergametest.socket.SocketServer;
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
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toMap;

public class FabricServerTestEngine implements TestEngine {

    public static final String ID = "cucumber-fabric-server";
    private final CucumberTestEngine delegate = new CucumberTestEngine();
    private final SocketServer server = new SocketServer();

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public TestDescriptor discover(EngineDiscoveryRequest request, UniqueId uniqueId) {
        return delegate.discover(request, uniqueId);
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
                    .filter(testDescriptor -> testDescriptor.getDisplayName().equals("Feature"))
                    .map(testDescriptor -> testDescriptor.getUniqueId().toString())
                    .toList();

            ConfigurationParameters configurationParameters = request.getConfigurationParameters();
            Map<String, String> stringParams = configurationParameters
                    .keySet()
                    .stream()
                    .flatMap(key -> configurationParameters.get(key).stream()
                            .map(value -> Map.entry(key, value)))
                    .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

            TestRequestPayload payload = new TestRequestPayload(uniqueIds, stringParams);

            while (!server.isClientConnected()) {
                server.poll();
                TimeUnit.MILLISECONDS.sleep(100);
            }

            EngineExecutionListener engineExecutionListener = request.getEngineExecutionListener();
            engineExecutionListener.executionStarted(request.getRootTestDescriptor());

            TimeUnit.MILLISECONDS.sleep(500);
            server.sendObject(payload);

            while (!runDone) {
                while (!server.hasMessages()) {
                    server.poll();
                    TimeUnit.MILLISECONDS.sleep(10);
                }
                Object newMessage = server.getNextMessage();
                if (newMessage instanceof ExecutionReply executionReply) {

                    Optional<? extends TestDescriptor> testDescriptorOptional;

                    if (Objects.equals(request.getRootTestDescriptor().getUniqueId().toString(), executionReply.getUniqueId())) {
                        testDescriptorOptional = Optional.of(request.getRootTestDescriptor());
                    } else {
                        testDescriptorOptional = request.getRootTestDescriptor().getDescendants().stream()
                                .filter(descriptor -> descriptor.getUniqueId().toString().equals(executionReply.getUniqueId()))
                                .findFirst();
                    }

                    if (testDescriptorOptional.isPresent()) {
                        TestDescriptor testDescriptor = testDescriptorOptional.get();

                        switch (executionReply.getExecutionType()) {
                            case DYNAMIC_TEST_REGISTERED ->
                                    engineExecutionListener.dynamicTestRegistered(testDescriptor);
                            case EXECUTION_SKIPPED ->
                                    engineExecutionListener.executionSkipped(testDescriptor, executionReply.getReason());
                            case EXECUTION_STARTED -> engineExecutionListener.executionStarted(testDescriptor);
                            case EXECUTION_FINISHED -> {
                                if (executionReply.toTestExecutionResult().isPresent()) {
                                    engineExecutionListener.executionFinished(testDescriptor, executionReply.toTestExecutionResult().get());
                                } else {
                                    throw new RuntimeException(String.format("Could not find TestExecutionResult for TestDescriptor with  %s", executionReply.getUniqueId()));
                                }
                            }
                            case REPORTING_ENTRY_PUBLISHED -> {
                                if (executionReply.keyValuePairs().isPresent()) {
                                    ReportEntry reportEntry = ReportEntry.from(executionReply.keyValuePairs().get());
                                    engineExecutionListener.reportingEntryPublished(testDescriptor, reportEntry);
                                } else {
                                    throw new RuntimeException(String.format("Could not find ReportEntry for TestDescriptor with  %s", executionReply.getUniqueId()));
                                }
                            }
                            case FILE_ENTRY_PUBLISHED -> {
                                if (executionReply.getFilePath().isPresent()) {
                                    FileEntry fileEntry = FileEntry.from(executionReply.getFilePath().get(), executionReply.getMediaType());
                                    engineExecutionListener.fileEntryPublished(testDescriptor, fileEntry);
                                } else {
                                    throw new RuntimeException(String.format("Could not find FileEntry for TestDescriptor with  %s", executionReply.getUniqueId()));
                                }
                            }
                        }
                    }
                } else if (newMessage instanceof MessageWrapper messageWrapper) {
                    if (messageWrapper.getMessageType().equals(MessageType.DONE)) {
                        runDone = true;
                    } else if (messageWrapper.getMessageType().equals(MessageType.ERROR)) {
                        throw new RuntimeException("Exception received from Mod", messageWrapper.getMessage());
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