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
            EngineUtils.startFabricKnot(EnvType.SERVER, "run");

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

                        EngineUtils.dispatchExecutionReply(executionReplyDTO, engineExecutionListener, testDescriptor);
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
}