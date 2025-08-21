package net.cucumberfabric.dto;

import net.cucumberfabric.dto.types.MessageType;

import java.io.Serial;
import java.io.Serializable;

public class MessageWrapperDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final MessageType messageType;
    private final Exception message;

    public MessageWrapperDTO(MessageType messageType, Exception message) {
        this.messageType = messageType;
        this.message = message;
    }

    public MessageWrapperDTO(MessageType messageType) {
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

