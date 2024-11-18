// BitOutputStream.java
import java.io.*;

public class BitOutputStream {
    private OutputStream out;
    private int currentByte;
    private int numBitsFilled;

    public BitOutputStream(OutputStream out) {
        this.out = out;
        this.currentByte = 0;
        this.numBitsFilled = 0;
    }

    public void write(int bits, int numBits) throws IOException {
        while (numBits > 0) {
            int bitsToWrite = Math.min(8 - numBitsFilled, numBits);
            int shiftedBits = (bits >> (numBits - bitsToWrite)) & ((1 << bitsToWrite) - 1);
            currentByte = (currentByte << bitsToWrite) | shiftedBits;
            numBitsFilled += bitsToWrite;
            numBits -= bitsToWrite;

            if (numBitsFilled == 8) {
                out.write(currentByte);
                numBitsFilled = 0;
                currentByte = 0;
            }
        }
    }

    public void close() throws IOException {
        if (numBitsFilled > 0) {
            currentByte <<= (8 - numBitsFilled);
            out.write(currentByte);
        }
        out.close();
    }
}