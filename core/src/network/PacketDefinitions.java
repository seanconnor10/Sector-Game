package network;

public class PacketDefinitions {
    private abstract class Packet {
        abstract void pack();
        abstract void unpack();
    }
}
