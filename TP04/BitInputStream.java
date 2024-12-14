// BitInputStream.java
import java.io.*;

public class BitInputStream {
    private InputStream in;
    private int currentByte;
    private int numBitsRemaining;

    public BitInputStream(InputStream in) {
        this.in = in;
        this.numBitsRemaining = 0;
    }

    public int read(int numBits) throws IOException {
        int result = 0;
        while (numBits > 0) {
            if (numBitsRemaining == 0) {
                currentByte = in.read();
                if (currentByte == -1) {
                    return -1;
                }
                numBitsRemaining = 8;
            }

            int bitsToRead = Math.min(numBitsRemaining, numBits);
            result = (result << bitsToRead) | ((currentByte >> (numBitsRemaining - bitsToRead)) & ((1 << bitsToRead) - 1));
            numBitsRemaining -= bitsToRead;
            numBits -= bitsToRead;
        }
        return result;
    }

    public void close() throws IOException {
        in.close();
    }
}
