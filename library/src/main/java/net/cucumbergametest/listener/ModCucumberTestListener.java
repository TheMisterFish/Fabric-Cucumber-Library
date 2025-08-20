package net.cucumbergametest.listener;

import net.cucumbergametest.dto.ExecutionReply;
import net.cucumbergametest.dto.ExecutionType;
import net.cucumbergametest.socket.SocketClient;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.reporting.FileEntry;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ModCucumberTestListener implements TestExecutionListener {
    private final Logger LOGGER = LoggerFactory.getLogger(ModCucumberTestListener.class);
    private final SocketClient socketClient;

    public ModCucumberTestListener(SocketClient socketClient) {
        this.socketClient = socketClient;
    }

    @Override
    public void dynamicTestRegistered(TestIdentifier testIdentifier) {
        TestExecutionListener.super.dynamicTestRegistered(testIdentifier);
        ExecutionReply executionReply = new ExecutionReply(testIdentifier.getUniqueId(), ExecutionType.DYNAMIC_TEST_REGISTERED);
        sendObject(executionReply);
    }

    @Override
    public void executionSkipped(TestIdentifier testIdentifier, String reason) {
        TestExecutionListener.super.executionSkipped(testIdentifier, reason);
        ExecutionReply executionReply = new ExecutionReply(testIdentifier.getUniqueId(), reason, ExecutionType.EXECUTION_SKIPPED);
        sendObject(executionReply);
    }

    @Override
    public void executionStarted(TestIdentifier testIdentifier) {
        TestExecutionListener.super.executionStarted(testIdentifier);
        ExecutionReply executionReply = new ExecutionReply(testIdentifier.getUniqueId(), ExecutionType.EXECUTION_STARTED);
        sendObject(executionReply);
    }

    @Override
    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
        TestExecutionListener.super.executionFinished(testIdentifier, testExecutionResult);
        ExecutionReply executionReply = new ExecutionReply(testIdentifier.getUniqueId(), testExecutionResult, ExecutionType.EXECUTION_FINISHED);
        sendObject(executionReply);
    }

    @Override
    public void reportingEntryPublished(TestIdentifier testIdentifier, ReportEntry entry) {
        TestExecutionListener.super.reportingEntryPublished(testIdentifier, entry);
        ExecutionReply executionReply = new ExecutionReply(testIdentifier.getUniqueId(), entry.getKeyValuePairs(), ExecutionType.REPORTING_ENTRY_PUBLISHED);
        sendObject(executionReply);
    }

    @Override
    public void fileEntryPublished(TestIdentifier testIdentifier, FileEntry file) {
        TestExecutionListener.super.fileEntryPublished(testIdentifier, file);
        if (file.getMediaType().isPresent()) {
            ExecutionReply executionReply = new ExecutionReply(testIdentifier.getUniqueId(), file.getPath(), file.getMediaType().get(), ExecutionType.FILE_ENTRY_PUBLISHED);
            sendObject(executionReply);
        } else {
            ExecutionReply executionReply = new ExecutionReply(testIdentifier.getUniqueId(), file.getPath(), "", ExecutionType.FILE_ENTRY_PUBLISHED);
            sendObject(executionReply);
        }
    }

    private void sendObject(ExecutionReply executionReply) {
        try {
            socketClient.sendObject(executionReply);
        } catch (IOException e) {
            LOGGER.error("Exception while trying to send ExecutionReply", e);
            throw new RuntimeException(e);
        }
    }
}
