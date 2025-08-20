package net.cucumbergametest.dto;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class MessageWrapper implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final MessageType messageType;
    private final Exception message;

    public MessageWrapper(MessageType messageType, Exception message) {
        this.messageType = messageType;
        this.message = message;
    }

    public MessageWrapper(MessageType messageType) {
        this.messageType = messageType;
        this.message = null;
    }


    public MessageType getMessageType() {
        return messageType;
    }

    public Exception getMessage() {
        return message;
    }
}

