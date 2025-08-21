package net.cucumberfabric.socket;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class ClientSocketHandler extends AbstractSocketHandler {
    private SocketChannel socketChannel;
    private boolean connected = false;

    public ClientSocketHandler() {
        super(ClientSocketHandler.class);
    }

    public void connect() throws IOException {
        int port = Integer.parseInt(System.getProperty("cucumber.socket.port"));
        socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);

        LOGGER.info("Connecting to server: 127.0.0.1:" + port);
        socketChannel.connect(new InetSocketAddress("127.0.0.1", port));
    }

    @Override
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
            readFromChannel(socketChannel);
        }
    }

    @Override
    public void sendObject(Serializable obj) throws IOException {
        byte[] serializedData = serializeObject(obj);
        sendSerializedData(serializedData, socketChannel);
    }

    @Override
    public boolean isConnected() {
        return connected && socketChannel != null && socketChannel.isConnected();
    }

    @Override
    public void close() throws IOException {
        connected = false;
        if (socketChannel != null) {
            socketChannel.close();
        }
    }

    private void readFromChannel(SocketChannel channel) throws IOException, ClassNotFoundException {
        if (dataBuffer == null) {
            int bytesRead = channel.read(lengthBuffer);
            if (bytesRead == -1) {
                channel.close();
                connected = false;
                return;
            }

            if (!lengthBuffer.hasRemaining()) {
                lengthBuffer.flip();
                int dataLength = lengthBuffer.getInt();
                dataBuffer = ByteBuffer.allocate(dataLength);
            }
        }

        if (dataBuffer != null) {
            int bytesRead = channel.read(dataBuffer);
            if (bytesRead == -1) {
                channel.close();
                connected = false;
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