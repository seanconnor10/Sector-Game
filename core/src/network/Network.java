package network;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Enumeration;

public class Network {
    public static Network Instance = new Network();

    public static final int SERVER_PORT = 9571;
    public static final int CLIENT_PORT = 9572;

    public DatagramChannel serverChannel;

    public DatagramChannel clientChannel;

    public InetSocketAddress clientSendAddress;
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
        Inet4Address address = (Inet4Address) getWifiAddress();
        try {
            serverChannel = DatagramChannel.open();
            serverChannel.bind(new InetSocketAddress(address, SERVER_PORT));
            serverChannel.configureBlocking(false);
        } catch (IOException e) {
            System.out.println("FAILED to open Server. Exception:" + e);
            return "FAILED to open Server. Exception:" + e;
        }

        getWifiAddress();

        System.out.println("Opened server at " + address.toString());
        return "Opened server at " + address;
    }

    public String connect(String address) {
        if (isHost()) {
            throw new NetworkException("Tried to connect as client when already host");
        }

        close();

        //try {
            clientSendAddress = new InetSocketAddress(getWifiAddress(), SERVER_PORT);
        //} catch (UnknownHostException e) {
        //    throw new NetworkException("Unknown host address given");
        //}

        try {
            clientChannel = DatagramChannel.open();
            clientChannel.configureBlocking(false);
        } catch (IOException e) {
            throw new NetworkException("Failed to create client socket");
        }

        return clientSendAddress.toString();

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
            clientChannel.send(bb, clientSendAddress);
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

    private InetAddress getWifiAddress() {
        Enumeration<NetworkInterface> interfaces = null;
        try {
            interfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }

        InetAddress addr = null;

        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();
            // drop inactive
            try {
                if (!networkInterface.isUp())
                    continue;
            } catch (SocketException e) {
                throw new RuntimeException(e);
            }

            // smth we can explore
            Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
            while(addresses.hasMoreElements()) {
                addr = addresses.nextElement();
                System.out.println(String.format("NetInterface: name [%s], ip [%s]",
                        networkInterface.getDisplayName(), addr.getHostAddress()));
            }
        }

        return addr;
    }
}
