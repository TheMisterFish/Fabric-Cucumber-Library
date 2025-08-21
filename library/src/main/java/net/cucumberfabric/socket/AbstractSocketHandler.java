package net.cucumberfabric.socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class AbstractSocketHandler {
    protected final Logger LOGGER;
    protected final Queue<Object> messageQueue = new ConcurrentLinkedQueue<>();
    protected final ByteBuffer lengthBuffer = ByteBuffer.allocate(4);
    protected ByteBuffer dataBuffer = null;

    protected AbstractSocketHandler(Class<?> clazz) {
        this.LOGGER = LoggerFactory.getLogger(clazz);
    }

    public abstract void poll() throws IOException, ClassNotFoundException;
    public abstract void sendObject(Serializable obj) throws IOException;
    public abstract void close() throws IOException;
    public abstract boolean isConnected();

    public Object getNextMessage() {
        return messageQueue.poll();
    }

    public boolean hasMessages() {
        return !messageQueue.isEmpty();
    }

    public void clearMessages() {
        messageQueue.clear();
    }

    protected Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data))) {
            return ois.readObject();
        }
    }

    protected void sendSerializedData(byte[] serializedData, SocketChannel channel) throws IOException {
        int length = serializedData.length;
        ByteBuffer buffer = ByteBuffer.allocate(4 + length);
        buffer.putInt(length);
        buffer.put(serializedData);
        buffer.flip();

        while (buffer.hasRemaining()) {
            channel.write(buffer);
        }
    }

    protected byte[] serializeObject(Serializable obj) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try (ObjectOutputStream objectStream = new ObjectOutputStream(byteStream)) {
            objectStream.writeObject(obj);
            objectStream.flush();
        }
        return byteStream.toByteArray();
    }
}