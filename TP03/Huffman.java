import java.io.*;
import java.util.*;

public class Huffman {

    private class Node implements Comparable<Node> {
        char ch;
        int freq;
        Node left, right;

        public Node(char ch, int freq, Node left, Node right) {
            this.ch = ch;
            this.freq = freq;
            this.left = left;
            this.right = right;
        }

        public int compareTo(Node other) {
            return this.freq - other.freq;
        }

        public boolean isLeaf() {
            return (left == null) && (right == null);
        }
    }

    public void compress(String inputFileName, String outputFileName) throws IOException {
        // Read input file
        FileInputStream fis = new FileInputStream(inputFileName);
        byte[] inputBytes = fis.readAllBytes();
        fis.close();

        // Build frequency table
        Map<Byte, Integer> freqMap = new HashMap<>();
        for (byte b : inputBytes) {
            freqMap.put(b, freqMap.getOrDefault(b, 0) + 1);
        }

        // Build Huffman tree
        Node root = buildHuffmanTree(freqMap);

        // Build code table
        Map<Byte, String> codeTable = new HashMap<>();
        buildCodeTable(codeTable, root, "");

        // Encode input data
        StringBuilder encodedData = new StringBuilder();
        for (byte b : inputBytes) {
            encodedData.append(codeTable.get(b));
        }

        // Convert encoded data to bytes
        BitSet bitSet = new BitSet(encodedData.length());
        for (int i = 0; i < encodedData.length(); i++) {
            if (encodedData.charAt(i) == '1') {
                bitSet.set(i);
            }
        }
        byte[] encodedBytes = bitSet.toByteArray();

        FileOutputStream fos = new FileOutputStream(outputFileName);
        ObjectOutputStream oos = new ObjectOutputStream(fos);

        oos.writeObject(freqMap);

        oos.writeInt(encodedData.length());

        oos.write(encodedBytes);

        oos.close();
    }

    private Node buildHuffmanTree(Map<Byte, Integer> freqMap) {
        PriorityQueue<Node> pq = new PriorityQueue<>();
        for (Map.Entry<Byte, Integer> entry : freqMap.entrySet()) {
            pq.add(new Node((char)(byte)entry.getKey(), entry.getValue(), null, null));
        }

        while (pq.size() > 1) {
            Node left = pq.poll();
            Node right = pq.poll();
            Node parent = new Node('\0', left.freq + right.freq, left, right);
            pq.add(parent);
        }

        return pq.poll();
    }

    private void buildCodeTable(Map<Byte, String> codeTable, Node node, String code) {
        if (!node.isLeaf()) {
            buildCodeTable(codeTable, node.left, code + '0');
            buildCodeTable(codeTable, node.right, code + '1');
        } else {
            codeTable.put((byte)node.ch, code);
        }
    }

    public void decompress(String inputFileName, String outputFileName) throws IOException, ClassNotFoundException {
        FileInputStream fis = new FileInputStream(inputFileName);
        ObjectInputStream ois = new ObjectInputStream(fis);
    
        // Read frequency table
        @SuppressWarnings("unchecked")
        Map<Byte, Integer> freqMap = (Map<Byte, Integer>) ois.readObject();
    
        // Rebuild Huffman tree
        Node root = buildHuffmanTree(freqMap);
    
        // Read encoded data length
        int encodedDataLength = ois.readInt();
    
        // Read encoded data
        byte[] encodedBytes = ois.readAllBytes();
        BitSet bitSet = BitSet.valueOf(encodedBytes);
    
        // Decode data
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Node current = root;
        for (int i = 0; i < encodedDataLength; i++) {
            if (bitSet.get(i)) {
                current = current.right;
            } else {
                current = current.left;
            }
    
            if (current.isLeaf()) {
                baos.write((byte) current.ch);
                current = root;
            }
        }
    
        // Write decompressed data to output file
        FileOutputStream fos = new FileOutputStream(outputFileName);
        baos.writeTo(fos);
        fos.close();
    
        ois.close();
    }    


}
