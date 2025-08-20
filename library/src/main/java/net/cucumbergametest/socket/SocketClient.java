package net.cucumbergametest.socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SocketClient {
    private final Logger LOGGER = LoggerFactory.getLogger(SocketClient.class);
    private SocketChannel socketChannel;
    private final Queue<Object> messageQueue = new ConcurrentLinkedQueue<>();

    private final ByteBuffer lengthBuffer = ByteBuffer.allocate(4);
    private ByteBuffer dataBuffer = null;

    private boolean connected = false;

    public void connect() throws IOException {
        int port = Integer.parseInt(System.getProperty("cucumber.socket.port"));

        socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);

        LOGGER.info("Connecting to server: 127.0.0.1:" + port);
        socketChannel.connect(new InetSocketAddress("127.0.0.1", port));
    }

    public void poll() throws IOException, ClassNotFoundException {
        if (!connected) {
            if (socketChannel.isConnectionPending()) {
                try {
                    socketChannel.finishConnect();
                    connected = true;
                    LOGGER.info("Connected to server");
                } catch (IOException e) {
                    LOGGER.error("Connection failed", e);
                }
            }
        }

        if (connected) {
            if (dataBuffer == null) {
                int bytesRead = socketChannel.read(lengthBuffer);
                if (bytesRead == -1) {
                    socketChannel.close();
                    socketChannel = null;
                    return;
                }

                if (!lengthBuffer.hasRemaining()) {
                    lengthBuffer.flip();
                    int dataLength = lengthBuffer.getInt();
                    dataBuffer = ByteBuffer.allocate(dataLength);
                }
            }

            if (dataBuffer != null) {
                int bytesRead = socketChannel.read(dataBuffer);
                if (bytesRead == -1) {
                    socketChannel.close();
                    socketChannel = null;
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
            socketChannel.write(buffer);
        }
    }

    public boolean isConnected() {
        return connected && socketChannel != null && socketChannel.isConnected();
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
        connected = false;
        if (socketChannel != null) {
            socketChannel.close();
        }
    }

    private Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data))) {
            return ois.readObject();
        }
    }
}