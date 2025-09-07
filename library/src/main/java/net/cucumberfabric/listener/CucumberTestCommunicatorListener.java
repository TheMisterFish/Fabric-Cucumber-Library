package net.cucumberfabric.listener;

import net.cucumberfabric.dto.ExecutionReplyDTO;
import net.cucumberfabric.dto.types.ExecutionType;
import net.cucumberfabric.socket.ClientSocketHandler;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.reporting.FileEntry;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class CucumberTestCommunicatorListener implements TestExecutionListener {
    private final Logger LOGGER = LoggerFactory.getLogger(CucumberTestCommunicatorListener.class);
    private final ClientSocketHandler clientSocketHandler;

    public CucumberTestCommunicatorListener(ClientSocketHandler clientSocketHandler) {
        this.clientSocketHandler = clientSocketHandler;
    }

    @Override
    public void dynamicTestRegistered(TestIdentifier testIdentifier) {
        TestExecutionListener.super.dynamicTestRegistered(testIdentifier);
        ExecutionReplyDTO executionReplyDTO = new ExecutionReplyDTO(testIdentifier.getUniqueId(), ExecutionType.DYNAMIC_TEST_REGISTERED);
        sendObject(executionReplyDTO);
    }

    @Override
    public void executionSkipped(TestIdentifier testIdentifier, String reason) {
        TestExecutionListener.super.executionSkipped(testIdentifier, reason);
        ExecutionReplyDTO executionReplyDTO = new ExecutionReplyDTO(testIdentifier.getUniqueId(), reason, ExecutionType.EXECUTION_SKIPPED);
        sendObject(executionReplyDTO);
    }

    @Override
    public void executionStarted(TestIdentifier testIdentifier) {
        TestExecutionListener.super.executionStarted(testIdentifier);
        ExecutionReplyDTO executionReplyDTO = new ExecutionReplyDTO(testIdentifier.getUniqueId(), ExecutionType.EXECUTION_STARTED);
        sendObject(executionReplyDTO);
    }

    @Override
    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
        TestExecutionListener.super.executionFinished(testIdentifier, testExecutionResult);
        ExecutionReplyDTO executionReplyDTO = new ExecutionReplyDTO(testIdentifier.getUniqueId(), testExecutionResult, ExecutionType.EXECUTION_FINISHED);
        sendObject(executionReplyDTO);
    }

    @Override
    public void reportingEntryPublished(TestIdentifier testIdentifier, ReportEntry entry) {
        TestExecutionListener.super.reportingEntryPublished(testIdentifier, entry);
        ExecutionReplyDTO executionReplyDTO = new ExecutionReplyDTO(testIdentifier.getUniqueId(), entry.getKeyValuePairs(), ExecutionType.REPORTING_ENTRY_PUBLISHED);
        sendObject(executionReplyDTO);
    }

    @Override
    public void fileEntryPublished(TestIdentifier testIdentifier, FileEntry file) {
        TestExecutionListener.super.fileEntryPublished(testIdentifier, file);
        if (file.getMediaType().isPresent()) {
            ExecutionReplyDTO executionReplyDTO = new ExecutionReplyDTO(testIdentifier.getUniqueId(), file.getPath(), file.getMediaType().get(), ExecutionType.FILE_ENTRY_PUBLISHED);
            sendObject(executionReplyDTO);
        } else {
            ExecutionReplyDTO executionReplyDTO = new ExecutionReplyDTO(testIdentifier.getUniqueId(), file.getPath(), "", ExecutionType.FILE_ENTRY_PUBLISHED);
            sendObject(executionReplyDTO);
        }
    }

    private void sendObject(ExecutionReplyDTO executionReplyDTO) {
        try {
            clientSocketHandler.sendObject(executionReplyDTO);
        } catch (IOException e) {
            LOGGER.error("Exception while trying to send ExecutionReply", e);
            throw new RuntimeException(e);
        }
    }
}
