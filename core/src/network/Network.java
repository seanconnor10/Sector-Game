package network;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class Network {
    public static Network Instance = new Network();

    public static final int SERVER_PORT = 9571;
    public static final int CLIENT_PORT = 9572;

    public DatagramChannel serverChannel;

    public DatagramChannel clientChannel;

    public InetAddress clientSendAddress;
    public String sendAddressIp = "";

    public byte[] receivedData = new byte[1024];
    public byte[] sendData = new byte[1024];

    public void step() {
        if (serverChannel != null && clientChannel != null) {
            throw new NetworkException("Attempting to be both client and server");
        }

        if (serverChannel == null && clientChannel == null) {
            return;
        }

        if (isHost()) {
            stepHost();
        } else {
            stepClient();
        }

    }

    public String openServer() {
        String address = "actually nope it failed";
        try {
            serverChannel = DatagramChannel.open();
            serverChannel.bind(new InetSocketAddress("localhost", SERVER_PORT));
            serverChannel.configureBlocking(false);
            address = serverChannel.getLocalAddress().toString();
        } catch (IOException e) {
            System.out.println("FAILED to open Server. Exception:" + e);
            return "FAILED to open Server. Exception:" + e;
        }

        System.out.println("Opened server at " + address);
        return "Opened server at " + address;
    }

    public void connect(String address) {
        if (isHost()) {
            throw new NetworkException("Tried to connect as client when already host");
        }

        close();

        try {
            clientSendAddress = Inet4Address.getByName(address/*+ SERVER_PORT*/);
        } catch (UnknownHostException e) {
            throw new NetworkException("Unknown host address given");
        }

        try {
            clientChannel = DatagramChannel.open();
            clientChannel.configureBlocking(false);
        } catch (IOException e) {
            throw new NetworkException("Failed to create client socket");
        }

    }

    public void close() {
        if (serverChannel != null) {
            try {
                serverChannel.close();
                serverChannel = null;
            } catch (IOException e) {
                //throw new RuntimeException(e);
            }
        }
        if (clientChannel != null) {
            try {
                clientChannel.close();
                clientChannel = null;
            } catch (IOException e) {
                //throw new RuntimeException(e);
            }
        }
    }

    private boolean isHost() {
        return serverChannel != null;
    }

    private void stepClient() {
        String message = Gdx.input.isKeyPressed(Input.Keys.W) ? "Key Is Pressed!" : "nope";
        sendData = message.getBytes();

        ByteBuffer bb = ByteBuffer.wrap(sendData);
        try {
            clientChannel.send(bb, new InetSocketAddress("localhost", SERVER_PORT));
        } catch (IOException e) {
            System.out.println("Exception sending as client");
        }
    }

    private void stepHost() {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            SocketAddress remoteAdd = serverChannel.receive(buffer);
            String message = extractMessage(buffer);
            System.out.println("Client at #" + remoteAdd + "  sent: " + message);
        } catch (IOException e) {
            System.out.println("Exception recieving messages");
        }
    }

    private String extractMessage(ByteBuffer buffer) {
        buffer.flip();

        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        return new String(bytes);
    }
}
