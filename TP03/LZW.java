import java.io.*;
import java.util.*;

public class LZW {

    public void compress(String inputFileName, String outputFileName) throws IOException {
        FileInputStream fis = new FileInputStream(inputFileName);
        byte[] inputBytes = fis.readAllBytes();
        fis.close();

        // Build the dictionary
        int dictSize = 256;
        Map<String, Integer> dictionary = new HashMap<>();
        for (int i = 0; i < dictSize; i++) {
            dictionary.put("" + (char) i, i);
        }

        String w = "";
        List<Integer> result = new ArrayList<>();

        for (byte b : inputBytes) {
            char c = (char) (b & 0xFF);
            String wc = w + c;
            if (dictionary.containsKey(wc)) {
                w = wc;
            } else {
                result.add(dictionary.get(w));
                // Add wc to the dictionary
                if (dictSize < 4096) { // Limit the size of the code to 12 bits (4096 bytes)
                    dictionary.put(wc, dictSize++);
                }
                w = "" + c;
            }
        }

        // Output the code for w.
        if (!w.equals("")) {
            result.add(dictionary.get(w));
        }

        FileOutputStream fos = new FileOutputStream(outputFileName);
        BitOutputStream bos = new BitOutputStream(fos);

        int maxBitLength = 12;

        for (int code : result) {
            bos.write(code, maxBitLength);
        }

        bos.close();
    }

    public void decompress(String inputFileName, String outputFileName) throws IOException {
        FileInputStream fis = new FileInputStream(inputFileName);
        BitInputStream bis = new BitInputStream(fis);
    
        // Build the dictionary
        int dictSize = 256;
        Map<Integer, String> dictionary = new HashMap<>();
        for (int i = 0; i < dictSize; i++) {
            dictionary.put(i, "" + (char) i);
        }
    
        int maxBitLength = 12;
    
        int prevCode = bis.read(maxBitLength);
        if (prevCode == -1) {
            bis.close();
            throw new IOException("Empty input file");
        }
    
        String w = dictionary.get(prevCode);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(w.getBytes());
    
        int code;
        while ((code = bis.read(maxBitLength)) != -1) {
            String entry;
            if (dictionary.containsKey(code)) {
                entry = dictionary.get(code);
            } else if (code == dictSize) {
                entry = w + w.charAt(0);
            } else {
                bis.close();
                throw new IOException("Bad compressed code: " + code);
            }
    
            baos.write(entry.getBytes());
    
            // Add w + entry[0] to the dictionary

            if (dictSize < 4096) {
                dictionary.put(dictSize++, w + entry.charAt(0));
            }
    
            w = entry;
        }
    
        bis.close();
    
        FileOutputStream fos = new FileOutputStream(outputFileName);
        baos.writeTo(fos);
        fos.close();
    }
    
}
