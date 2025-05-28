package network;

public class ByteHelpers {
    public static byte[] asBytes(float n) {
        int intBits =  Float.floatToIntBits(n);
        return new byte[] {
                (byte) (intBits >> 24), (byte) (intBits >> 16), (byte) (intBits >> 8), (byte) (intBits) };
    }
}
