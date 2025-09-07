package net.cucumberfabric.socket;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class ServerSocketHandler extends AbstractSocketHandler {
    private ServerSocketChannel serverSocketChannel;
    private SocketChannel clientChannel;

    public ServerSocketHandler() {
        super(ServerSocketHandler.class);
    }

    public int start() throws IOException {
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.socket().bind(new InetSocketAddress(0));

        return serverSocketChannel.socket().getLocalPort();
    }

    @Override
    public void poll() throws IOException, ClassNotFoundException {
        if (clientChannel == null) {
            clientChannel = serverSocketChannel.accept();
            if (clientChannel != null) {
                clientChannel.configureBlocking(false);
                LOGGER.debug("Client connected");
            }
        }

        if (clientChannel != null && clientChannel.isConnected()) {
            readFromChannel(clientChannel);
        }
    }

    @Override
    public void sendObject(Serializable obj) throws IOException {
        byte[] serializedData = serializeObject(obj);
        sendSerializedData(serializedData, clientChannel);
    }

    @Override
    public boolean isConnected() {
        return clientChannel != null && clientChannel.isConnected();
    }

    @Override
    public void close() throws IOException {
        if (clientChannel != null) {
            clientChannel.close();
        }
        if (serverSocketChannel != null) {
            serverSocketChannel.close();
        }
    }

    private void readFromChannel(SocketChannel channel) throws IOException, ClassNotFoundException {
        if (dataBuffer == null) {
            int bytesRead = channel.read(lengthBuffer);
            if (bytesRead == -1) {
                channel.close();
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
            int bytesRead = channel.read(dataBuffer);
            if (bytesRead == -1) {
                channel.close();
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