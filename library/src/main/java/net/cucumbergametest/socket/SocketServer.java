package net.cucumbergametest.socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SocketServer {
    private final Logger LOGGER = LoggerFactory.getLogger(SocketServer.class);

    private ServerSocketChannel serverSocketChannel;
    private SocketChannel clientChannel;
    private final Queue<Object> messageQueue = new ConcurrentLinkedQueue<>();
    private final ByteBuffer lengthBuffer = ByteBuffer.allocate(4);
    private ByteBuffer dataBuffer = null;

    public void start() throws IOException {
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.socket().bind(new InetSocketAddress(0));

        int port = serverSocketChannel.socket().getLocalPort();
        System.setProperty("cucumber.socket.port", String.valueOf(port));
    }

    public void poll() throws IOException, ClassNotFoundException {
        if (clientChannel == null) {
            clientChannel = serverSocketChannel.accept();
            if (clientChannel != null) {
                clientChannel.configureBlocking(false);
                LOGGER.debug("Client connected");
            }
        }

        if (clientChannel != null && clientChannel.isConnected()) {
            if (dataBuffer == null) {
                int bytesRead = clientChannel.read(lengthBuffer);
                if (bytesRead == -1) {
                    clientChannel.close();
                    clientChannel = null;
                    return;
                }

                if (!lengthBuffer.hasRemaining()) {
                    lengthBuffer.flip();
                    int dataLength = lengthBuffer.getInt();
                    dataBuffer = ByteBuffer.allocate(dataLength);
                }
            }

            if (dataBuffer != null) {
                int bytesRead = clientChannel.read(dataBuffer);
                if (bytesRead == -1) {
                    clientChannel.close();
                    clientChannel = null;
                    return;
                }

                if (!dataBuffer.hasRemaining()) {
                    dataBuffer.flip();
                    byte[] data = new byte[dataBuffer.remaining()];
                    dataBuffer.get(data);

                    Object dto = deserialize(data);
                    messageQueue.add(dto);

                    lengthBuffer.clear();
                    dataBuffer = null;
                }
            }
        }
    }

    public void sendObject(Serializable obj) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try (ObjectOutputStream objectStream = new ObjectOutputStream(byteStream)) {
            objectStream.writeObject(obj);
            objectStream.flush();
        }

        byte[] serializedData = byteStream.toByteArray();
        int length = serializedData.length;

        ByteBuffer buffer = ByteBuffer.allocate(4 + length);
        buffer.putInt(length);
        buffer.put(serializedData);
        buffer.flip();

        while (buffer.hasRemaining()) {
            clientChannel.write(buffer);
        }
    }

    public boolean isClientConnected() {
        return clientChannel != null && clientChannel.isConnected();
    }

    public Object getNextMessage() {
        return messageQueue.poll();
    }

    public boolean hasMessages() {
        return !messageQueue.isEmpty();
    }

    public void clearMessages() {
        messageQueue.clear();
    }

    public void close() throws IOException {
        if (clientChannel != null) {
            clientChannel.close();
        }
        if (serverSocketChannel != null) {
            serverSocketChannel.close();
        }
    }

    private Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data))) {
            return ois.readObject();
        }
    }
}