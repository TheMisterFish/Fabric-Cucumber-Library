package net.cucumberfabric.dto;

import java.io.Serial;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import net.cucumberfabric.dto.types.ExecutionType;
import org.junit.platform.engine.TestExecutionResult;

public class ExecutionReplyDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String uniqueId;
    private final ExecutionType executionType;

    private Path filePath;
    private String mediaType;
    private Map<String, String> keyValuePairs;
    private String reason;

    private TestExecutionResult.Status testStatus;
    private Throwable              testThrowable;

    public ExecutionReplyDTO(String uniqueId, ExecutionType executionType) {
        this.uniqueId      = uniqueId;
        this.executionType = executionType;
    }

    public ExecutionReplyDTO(String uniqueId, String reason, ExecutionType executionType) {
        this(uniqueId, executionType);
        this.reason = reason;
    }

    public ExecutionReplyDTO(String uniqueId,
                             Path filePath,
                             String mediaType,
                             ExecutionType executionType) {
        this(uniqueId, executionType);
        this.filePath  = filePath;
        this.mediaType = mediaType;
    }

    public ExecutionReplyDTO(String uniqueId,
                             Path filePath,
                             ExecutionType executionType) {
        this(uniqueId, executionType);
        this.filePath = filePath;
    }

    public ExecutionReplyDTO(String uniqueId,
                             Map<String, String> keyValuePairs,
                             ExecutionType executionType) {
        this(uniqueId, executionType);
        this.keyValuePairs = keyValuePairs;
    }

    public ExecutionReplyDTO(String uniqueId,
                             TestExecutionResult result,
                             ExecutionType executionType) {
        this(uniqueId, executionType);

        this.testStatus    = result.getStatus();
        this.testThrowable = result.getThrowable().orElse(null);
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public ExecutionType getExecutionType() {
        return executionType;
    }

    public Optional<Path> getFilePath() {
        return Optional.ofNullable(filePath);
    }

    public String getMediaType() {
        return mediaType;
    }

    public Optional<Map<String, String>> keyValuePairs() {
        return Optional.ofNullable(keyValuePairs);
    }

    public String getReason() {
        return reason;
    }

    public Optional<TestExecutionResult.Status> getTestStatus() {
        return Optional.ofNullable(testStatus);
    }

    public Optional<Throwable> getTestThrowable() {
        return Optional.ofNullable(testThrowable);
    }

    public Optional<TestExecutionResult> toTestExecutionResult() {
        if (testStatus == null) {
            return Optional.empty();
        }

        return switch (testStatus) {
            case SUCCESSFUL -> Optional.of(TestExecutionResult.successful());
            case ABORTED -> Optional.of(TestExecutionResult.aborted(testThrowable));
            case FAILED -> Optional.of(TestExecutionResult.failed(testThrowable));
        };
    }
}