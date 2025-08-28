package net.cucumberfabric.exception;

public class FabricEngineException extends RuntimeException {
    public FabricEngineException(String message) {
        super(message);
    }

    public FabricEngineException(String message, Throwable e) {
        super(message, e);
    }
}
