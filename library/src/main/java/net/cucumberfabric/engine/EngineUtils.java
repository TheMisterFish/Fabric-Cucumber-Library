package net.cucumberfabric.engine;

import net.cucumberfabric.dto.ExecutionReplyDTO;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.impl.launch.knot.Knot;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.reporting.FileEntry;
import org.junit.platform.engine.reporting.ReportEntry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class EngineUtils {

    public static void startFabricKnot(EnvType envType, String runDirectory) {
        try {
            ClassLoader currentLoader = Thread.currentThread().getContextClassLoader();
            // 1) prepare a sandbox run directory
            Path base = Paths.get("cucumber_run");
            Path run = base.resolve(runDirectory);
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
            }, envType);

            Thread.currentThread().setContextClassLoader(currentLoader);

        } catch (IOException ioe) {
            throw new RuntimeException("Failed to prepare Fabric run directory", ioe);
        }
    }

    public static void dispatchExecutionReply(ExecutionReplyDTO executionReplyDTO, EngineExecutionListener engineExecutionListener, TestDescriptor testDescriptor) {
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
