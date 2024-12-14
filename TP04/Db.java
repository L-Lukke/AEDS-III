import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class Db {

    private static final String VIGENERE_KEY = "MINHACHAVE";
    private boolean useDES = false; // se true, usa DES, senão usa Vigenère
    private SecretKey desKey;

    Hash hashIndex;
    BTree btreeIndex;
    FullInvertedIndex fullInvertedIndex;
    private int compressionVersion = 1;
    private int lastId = 0;
    public RandomAccessFile raf;

    public Db() {
        try {
            this.raf = new RandomAccessFile("output/raf.bin", "rw");
            this.fullInvertedIndex = new FullInvertedIndex("output/inverted/invertedIndex.csv");

            if (raf.length() != 0) {
                raf.seek(0);
                lastId = raf.readInt();
            }
            
            this.hashIndex = new Hash("output/hash/hashIndex.csv");
            this.btreeIndex = new BTree("output/btree/btreeIndex.csv");

            // Gera uma chave DES para a criptografia DES
            generateDESKey();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setEncryptionModeDES(boolean useDES) {
        this.useDES = useDES;
    }

    private void generateDESKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("DES");
        keyGen.init(56); // chave DES de 56 bits
        desKey = keyGen.generateKey();
    }

    public void initializeDatabase() {
        try {
            if (raf.length() == 0) {
                raf.seek(0);
                raf.writeInt(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getLastId() {
        return lastId;
    }

    public void databaseToBinary(Hero hero, boolean hash, boolean btree) {
        BufferedReader br = null;

        try {
            try {
                br = new BufferedReader(new InputStreamReader(new FileInputStream("input/Heroes.csv"), "iso-8859-1"));
            } catch (Exception e) {
                try {
                    br = new BufferedReader(new InputStreamReader(new FileInputStream("input/Heroes.csv"), "utf-8"));
                } catch (Exception e2) {
                    e.printStackTrace();
                }
            }

            System.out.println();
            System.out.println("Importing DB. This may take a while.");

            String line = br.readLine(); // Skip header
            line = br.readLine(); // first data line

            if (hash) {
                while (line != null) {
                    Hero.read(line, hero);
                    createHash(hero);
                    line = br.readLine();
                }
            }
            else if (btree) {
                while (line != null) {
                    Hero.read(line, hero);
                    createBTree(hero);
                    line = br.readLine();
                }
            }
            else {
                while (line != null) {
                    Hero.read(line, hero);
                    create(hero);
                    line = br.readLine();
                }
            }

            System.out.println("DB imported successfully");
            System.out.println();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Hero> stringMatchFile(String variable, String searchString, char algorithm) {
        List<Hero> matchingHeroes = new ArrayList<>();
        byte[] heroByte;

        try {
            raf.seek(0);
            raf.readInt();

            try (BufferedWriter writer = new BufferedWriter(new FileWriter("output/stringMatch/matchedHeroes.csv"))) {

                while (raf.getFilePointer() < raf.length()) {
                    byte tombstone = raf.readByte();
                    int size = raf.readInt();

                    if (tombstone == 1) {
                        raf.seek(raf.getFilePointer() + size);
                        continue;
                    }

                    heroByte = new byte[size];
                    raf.read(heroByte);

                    heroByte = decryptBytes(heroByte, VIGENERE_KEY); // decriptografa com base no modo atual
                    Hero hero = Hero.fromByteArray(heroByte);

                    String valueToSearch = "";
                    switch (variable) {
                        case "name":
                            valueToSearch = hero.getName();
                            break;
                        case "identity":
                            valueToSearch = hero.getIdentity();
                            break;
                        case "alignment":
                            valueToSearch = hero.getAlignment();
                            break;
                        case "eye color":
                            valueToSearch = hero.getEyeColor();
                            break;
                        case "hair color":
                            valueToSearch = hero.getHairColor();
                            break;
                        case "gender":
                            valueToSearch = hero.getGender();
                            break;
                        case "status":
                            valueToSearch = hero.getStatus();
                            break;
                        case "universe":
                            valueToSearch = hero.getUniverse();
                            break;
                        default:
                            System.out.println("Invalid variable selected. Please try again.");
                            return matchingHeroes;
                    }

                    boolean isMatch = false;
                    if (algorithm == 'k') {
                        isMatch = kmpSearch(valueToSearch.toLowerCase(), searchString.toLowerCase());
                    } else if (algorithm == 'b') {
                        isMatch = boyerMooreSearch(valueToSearch.toLowerCase(), searchString.toLowerCase());
                    } else {
                        System.out.println("Invalid algorithm selected. Please try again.");
                        return matchingHeroes;
                    }

                    if (isMatch) {
                        matchingHeroes.add(hero);
                        writer.write(hero.toString() + "\n");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return matchingHeroes;
    }

    public List<Hero> stringMatch(String variable, String searchString, char algorithm) {
        List<Hero> matchingHeroes = new ArrayList<>();
        byte[] heroByte;
    
        try {
            raf.seek(0);
            raf.readInt();

            while (raf.getFilePointer() < raf.length()) {
                byte tombstone = raf.readByte();
                int size = raf.readInt();

                if (tombstone == 1) {
                    raf.seek(raf.getFilePointer() + size);
                    continue;
                }

                heroByte = new byte[size];
                raf.read(heroByte);

                heroByte = decryptBytes(heroByte, VIGENERE_KEY);
                Hero hero = Hero.fromByteArray(heroByte);
    
                String valueToSearch = "";
                switch (variable) {
                    case "name":
                        valueToSearch = hero.getName();
                        break;
                    case "identity":
                        valueToSearch = hero.getIdentity();
                        break;
                    case "alignment":
                        valueToSearch = hero.getAlignment();
                        break;
                    case "eye color":
                        valueToSearch = hero.getEyeColor();
                        break;
                    case "hair color":
                        valueToSearch = hero.getHairColor();
                        break;
                    case "gender":
                        valueToSearch = hero.getGender();
                        break;
                    case "status":
                        valueToSearch = hero.getStatus();
                        break;
                    case "universe":
                        valueToSearch = hero.getUniverse();
                        break;
                    default:
                        System.out.println("Invalid variable selected. Please try again.");
                        return null;
                }

                boolean isMatch = false;
                if (algorithm == 'k') {
                    isMatch = kmpSearch(valueToSearch.toLowerCase(), searchString.toLowerCase());
                } else if (algorithm == 'b') {
                    isMatch = boyerMooreSearch(valueToSearch.toLowerCase(), searchString.toLowerCase());
                } else {
                    System.out.println("Invalid algorithm selected.");
                    return matchingHeroes;
                }
    
                if (isMatch) {
                    matchingHeroes.add(hero);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    
        return matchingHeroes;
    }

    // KMP
    private boolean kmpSearch(String text, String pattern) {
        int[] lps = computeLPSArray(pattern);
        int i = 0, j = 0;

        while (i < text.length()) {
            if (pattern.charAt(j) == text.charAt(i)) {
                i++;
                j++;
            }
            if (j == pattern.length()) {
                return true;
            } else if (i < text.length() && pattern.charAt(j) != text.charAt(i)) {
                if (j != 0) j = lps[j - 1];
                else i++;
            }
        }
        return false;
    }

    private int[] computeLPSArray(String pattern) {
        int[] lps = new int[pattern.length()];
        int length = 0;
        int i = 1;

        while (i < pattern.length()) {
            if (pattern.charAt(i) == pattern.charAt(length)) {
                length++;
                lps[i] = length;
                i++;
            } else {
                if (length != 0) length = lps[length - 1];
                else {
                    lps[i] = 0;
                    i++;
                }
            }
        }
        return lps;
    }

    // Boyer-Moore
    private boolean boyerMooreSearch(String text, String pattern) {
        int[] badChar = preprocessBoyerMoore(pattern);
        int shift = 0;

        while (shift <= text.length() - pattern.length()) {
            int j = pattern.length() - 1;

            while (j >= 0 && pattern.charAt(j) == text.charAt(shift + j)) {
                j--;
            }

            if (j < 0) {
                return true;
            } else {
                shift += Math.max(1, j - badChar[text.charAt(shift + j)]);
            }
        }
        return false;
    }

    private int[] preprocessBoyerMoore(String pattern) {
        int[] badChar = new int[256];
        Arrays.fill(badChar, -1);

        for (int i = 0; i < pattern.length(); i++) {
            badChar[pattern.charAt(i)] = i;
        }
        return badChar;
    }

    // Hash
    public boolean createHash(Hero hero) {
        try {
            if (raf.length() == 0) {
                lastId = 1;
                raf.seek(0);
                raf.writeInt(lastId);
            } else {
                raf.seek(0);
                lastId = raf.readInt();
                lastId++;
                raf.seek(0);
                raf.writeInt(lastId);
            }

            byte[] ba = hero.toByteArray();
            ba = encryptBytes(ba, VIGENERE_KEY);

            long recordAddress = raf.length();
            raf.seek(recordAddress);
            raf.writeByte(0);
            raf.writeInt(ba.length);
            raf.write(ba);

            hashIndex.addToHashIndex(hero.getId(), recordAddress);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Hero readHash(int id) {
        try {
            long address = hashIndex.searchTokenInHashIndex(id);

            if (address != -1) {
                raf.seek(address);
                byte tombstone = raf.readByte();

                if (tombstone == 1) {
                    return null;
                }

                int size = raf.readInt();
                byte[] heroByte = new byte[size];
                raf.read(heroByte);

                heroByte = decryptBytes(heroByte, VIGENERE_KEY);
                Hero hero = Hero.fromByteArray(heroByte);
                return hero;
            }

            return null;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean updateHash(int id, Hero updatedHero) {
        try {
            long address = hashIndex.searchTokenInHashIndex(id);

            if (address != -1) {
                raf.seek(address);
                byte tombstone = raf.readByte();

                if (tombstone == 1) {
                    return false;
                }

                int oldSize = raf.readInt();

                byte[] updatedBytes = updatedHero.toByteArray();
                updatedBytes = encryptBytes(updatedBytes, VIGENERE_KEY);

                if (updatedBytes.length <= oldSize) {
                    raf.seek(address + 1 + 4);
                    raf.write(updatedBytes);
                } else {
                    raf.seek(address);
                    raf.writeByte(1);

                    long newAddress = raf.length();
                    raf.seek(newAddress);
                    raf.writeByte(0);
                    raf.writeInt(updatedBytes.length);
                    raf.write(updatedBytes);

                    hashIndex.removeFromHashIndex(id);
                    hashIndex.addToHashIndex(id, newAddress);
                }

                return true;
            }

            return false;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteHash(int id) {
        try {
            hashIndex.removeFromHashIndex(id);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void printHashState() {
        hashIndex.printState();
    }

    // BTree
    public boolean createBTree(Hero hero) {
        try {
            if (raf.length() == 0) {
                lastId = 1;
                raf.seek(0);
                raf.writeInt(lastId);
            } else {
                raf.seek(0);
                lastId = raf.readInt();
                lastId++;
                raf.seek(0);
                raf.writeInt(lastId);
            }

            byte[] ba = hero.toByteArray();
            ba = encryptBytes(ba, VIGENERE_KEY);

            long recordAddress = raf.length();
            raf.seek(recordAddress);
            raf.writeByte(0);
            raf.writeInt(ba.length);
            raf.write(ba);

            btreeIndex.insert(hero.getId(), recordAddress);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Hero readBTree(int id) {
        try {
            long address = btreeIndex.search(id);

            if (address != -1) {
                raf.seek(address);
                byte tombstone = raf.readByte();

                if (tombstone == 1) {
                    return null;
                }

                int size = raf.readInt();
                byte[] heroByte = new byte[size];
                raf.read(heroByte);

                heroByte = decryptBytes(heroByte, VIGENERE_KEY);
                Hero hero = Hero.fromByteArray(heroByte);
                return hero;
            }

            return null;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean updateBTree(int id, Hero updatedHero) {
        try {
            long address = btreeIndex.search(id);

            if (address != -1) {
                raf.seek(address);
                byte tombstone = raf.readByte();

                if (tombstone == 1) {
                    return false;
                }

                int oldSize = raf.readInt();
                byte[] updatedBytes = updatedHero.toByteArray();
                updatedBytes = encryptBytes(updatedBytes, VIGENERE_KEY);

                if (updatedBytes.length <= oldSize) {
                    raf.seek(address + 1 + 4);
                    raf.write(updatedBytes);
                } else {
                    raf.seek(address);
                    raf.writeByte(1);

                    long newAddress = raf.length();
                    raf.seek(newAddress);
                    raf.writeByte(0);
                    raf.writeInt(updatedBytes.length);
                    raf.write(updatedBytes);

                    btreeIndex.insert(id, newAddress);
                }

                return true;
            }

            return false;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteBTree(int id) {
        try {
            btreeIndex.remove(id);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void printBtreeState() {
        btreeIndex.printState();
    }

    // Linear (DEPRECATED)
    public boolean create(Hero hero) {
        try {
            if (raf.length() == 0) {
                lastId = 1;
                raf.seek(0);
                raf.writeInt(lastId);
            } else {
                raf.seek(0);
                lastId = raf.readInt();
                lastId++;
                raf.seek(0);
                raf.writeInt(lastId);
            }

            byte[] ba = hero.toByteArray();
            ba = encryptBytes(ba, VIGENERE_KEY);

            raf.seek(raf.length());
            raf.writeByte(0);
            raf.writeInt(ba.length);
            raf.write(ba);

            fullInvertedIndex.add(hero);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Hero read(int id) {
        try {
            raf.seek(4);
            while (raf.getFilePointer() < raf.length()) {
                byte tombstone = raf.readByte();
                int size = raf.readInt();

                if (tombstone == 1) {
                    raf.seek(raf.getFilePointer() + size);
                    continue;
                }

                byte[] heroByte = new byte[size];
                raf.read(heroByte);

                heroByte = decryptBytes(heroByte, VIGENERE_KEY);
                Hero hero = Hero.fromByteArray(heroByte);
                if (hero.getId() == id) {
                    return hero;
                }
            }

            return null;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean update(int id, Hero updatedHero) {
        try {
            raf.seek(4);
            while (raf.getFilePointer() < raf.length()) {
                long recordPosition = raf.getFilePointer();
                byte tombstone = raf.readByte();
                int size = raf.readInt();

                if (tombstone == 1) {
                    raf.seek(raf.getFilePointer() + size);
                    continue;
                }

                byte[] heroByte = new byte[size];
                raf.read(heroByte);

                // Descriptografar para checar ID
                byte[] originalByte = Arrays.copyOf(heroByte, heroByte.length);
                originalByte = decryptBytes(originalByte, VIGENERE_KEY);
                Hero hero = Hero.fromByteArray(originalByte);

                if (hero.getId() == id) {
                    Hero oldHero = hero;
                    fullInvertedIndex.update(oldHero, updatedHero);

                    byte[] updatedBytes = updatedHero.toByteArray();
                    updatedBytes = encryptBytes(updatedBytes, VIGENERE_KEY);

                    if (updatedBytes.length <= size) {
                        raf.seek(recordPosition + 1 + 4);
                        raf.write(updatedBytes);
                    } else {
                        raf.seek(recordPosition);
                        raf.writeByte(1);

                        raf.seek(raf.length());
                        raf.writeByte(0);
                        raf.writeInt(updatedBytes.length);
                        raf.write(updatedBytes);
                    }
                    return true;
                }
            }

            return false;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void reinitialize() {
        try {
            if (raf != null) {
                raf.close();
            }

            raf = new RandomAccessFile("output/raf.bin", "rw");
            lastId = 0;
            raf.writeInt(lastId);

            hashIndex = new Hash("output/hash/hashIndex.csv");
            btreeIndex = new BTree("output/btree/btreeIndex.csv");
            fullInvertedIndex = new FullInvertedIndex("output/inverted/invertedIndex.csv");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean delete(int id) {
        try {
            raf.seek(4);
            while (raf.getFilePointer() < raf.length()) {
                long recordPosition = raf.getFilePointer();
                byte tombstone = raf.readByte();
                int size = raf.readInt();

                if (tombstone == 1) {
                    raf.seek(raf.getFilePointer() + size);
                    continue;
                }

                byte[] heroByte = new byte[size];
                raf.read(heroByte);

                byte[] originalByte = decryptBytes(heroByte, VIGENERE_KEY);
                Hero hero = Hero.fromByteArray(originalByte);

                if (hero.getId() == id) {
                    raf.seek(recordPosition);
                    raf.writeByte(1);
                    fullInvertedIndex.remove(hero);
                    return true;
                }
            }

            return false;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Hero> searchHeroes(String[] keywords) {
        List<Set<Integer>> keywordIdSets = new ArrayList<>();

        for (String keyword : keywords) {
            List<Integer> ids = fullInvertedIndex.search(keyword.toLowerCase());
            Set<Integer> idSet = new HashSet<>(ids);
            keywordIdSets.add(idSet);
        }

        Set<Integer> resultIds;
        if (!keywordIdSets.isEmpty()) {
            resultIds = keywordIdSets.get(0);
            for (int i = 1; i < keywordIdSets.size(); i++) {
                resultIds.retainAll(keywordIdSets.get(i));
            }
        } else {
            resultIds = new HashSet<>();
        }

        List<Hero> results = new ArrayList<>();
        for (int id : resultIds) {
            Hero hero = read(id);
            if (hero != null) {
                results.add(hero);
            }
        }

        return results;
    }

    public boolean massDelete() {
        try {
            raf.setLength(0);
            lastId = 0;
            hashIndex = new Hash("output/hash/hashIndex.csv");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void close() {
        try {
            if (raf != null) {
                raf.close();
                raf = null;
            }
            if (fullInvertedIndex != null) {
                fullInvertedIndex.close();
                fullInvertedIndex = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void reopen() {
        try {
            this.raf = new RandomAccessFile("output/raf.bin", "rw");
    
            if (raf.length() != 0) {
                raf.seek(0);
                lastId = raf.readInt();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void compressDatabase() {
        createDirectories();

        String inputFileName = "output/raf.bin";

        try {
            Huffman huffman = new Huffman();
            String huffmanOutputFileName = "output/compressed/raf.binHuffmanCompression" + compressionVersion;

            long huffmanStartTime = System.currentTimeMillis();
            huffman.compress(inputFileName, huffmanOutputFileName);
            long huffmanEndTime = System.currentTimeMillis();
            long huffmanTime = huffmanEndTime - huffmanStartTime;

            File inputFile = new File(inputFileName);
            File huffmanFile = new File(huffmanOutputFileName);
            long inputFileSize = inputFile.length();
            long huffmanFileSize = huffmanFile.length();

            double huffmanCompressionRatio = ((double) (inputFileSize - huffmanFileSize) / inputFileSize) * 100;

            LZW lzw = new LZW();
            String lzwOutputFileName = "output/compressed/raf.binLZWCompression" + compressionVersion;

            long lzwStartTime = System.currentTimeMillis();
            lzw.compress(inputFileName, lzwOutputFileName);
            long lzwEndTime = System.currentTimeMillis();
            long lzwTime = lzwEndTime - lzwStartTime;

            File lzwFile = new File(lzwOutputFileName);
            long lzwFileSize = lzwFile.length();

            double lzwCompressionRatio = ((double) (inputFileSize - lzwFileSize) / inputFileSize) * 100;

            System.out.println("Huffman Compression:");
            System.out.println("Time taken: " + huffmanTime + " ms");
            System.out.println("Original size: " + inputFileSize + " bytes");
            System.out.println("Compressed size: " + huffmanFileSize + " bytes");
            System.out.println("Compression ratio: " + String.format("%.2f", huffmanCompressionRatio) + "%");

            System.out.println();

            System.out.println("LZW Compression:");
            System.out.println("Time taken: " + lzwTime + " ms");
            System.out.println("Original size: " + inputFileSize + " bytes");
            System.out.println("Compressed size: " + lzwFileSize + " bytes");
            System.out.println("Compression ratio: " + String.format("%.2f", lzwCompressionRatio) + "%");

            System.out.println();

            if (huffmanCompressionRatio > lzwCompressionRatio) {
                System.out.println("Huffman compression performed better for this data.");
            } else if (lzwCompressionRatio > huffmanCompressionRatio) {
                System.out.println("LZW compression performed better for this data.");
            } else {
                System.out.println("Both algorithms performed equally well.");
            }

            compressionVersion++;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createDirectories() {
        File outputDir = new File("output");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        File compressedDir = new File("output/compressed");
        if (!compressedDir.exists()) {
            compressedDir.mkdirs();
        }

        File decompressedDir = new File("output/decompressed");
        if (!decompressedDir.exists()) {
            decompressedDir.mkdirs();
        }
    }

    public void decompressDatabase(int version) {
        String huffmanInputFileName = "output/compressed/raf.binHuffmanCompression" + version;
        String lzwInputFileName = "output/compressed/raf.binLZWCompression" + version;
        String outputFileNameHuffman = "output/decompressed/raf_decompressed_huffman.bin";
        String outputFileNameLZW = "output/decompressed/raf_decompressed_lzw.bin";

        try {
            Huffman huffman = new Huffman();

            long huffmanStartTime = System.currentTimeMillis();
            huffman.decompress(huffmanInputFileName, outputFileNameHuffman);
            long huffmanEndTime = System.currentTimeMillis();
            long huffmanTime = huffmanEndTime - huffmanStartTime;

            LZW lzw = new LZW();

            long lzwStartTime = System.currentTimeMillis();
            lzw.decompress(lzwInputFileName, outputFileNameLZW);
            long lzwEndTime = System.currentTimeMillis();
            long lzwTime = lzwEndTime - lzwStartTime;

            System.out.println("Huffman Decompression:");
            System.out.println("Time taken: " + huffmanTime + " ms");
            System.out.println("Compressed size: " + new File(huffmanInputFileName).length() + " bytes");
            System.out.println("Decompressed size: " + new File(outputFileNameHuffman).length() + " bytes");
            System.out.println();

            System.out.println("LZW Decompression:");
            System.out.println("Time taken: " + lzwTime + " ms");
            System.out.println("Compressed size: " + new File(lzwInputFileName).length() + " bytes");
            System.out.println("Decompressed size: " + new File(outputFileNameLZW).length() + " bytes");
            System.out.println();

            if (huffmanTime <= lzwTime) {
                System.out.println("Huffman decompression was faster for this data.");
            } else {
                System.out.println("LZW decompression was faster for this data.");
            }

            close();

            System.gc();
            Thread.sleep(100);

            File rafFile = new File("output/raf.bin");
            if (huffmanTime <= lzwTime) {
                Files.copy(Paths.get(outputFileNameHuffman), rafFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Database has been replaced with the decompressed data from Huffman.");
            } else {
                Files.copy(Paths.get(outputFileNameLZW), rafFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Database has been replaced with the decompressed data from LZW.");
            }

            reopen();

        } catch (IOException e) {
            System.out.println("Compressed file not found for the given version in the 'output/compressed/' directory.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Métodos de criptografia
    private byte[] encryptBytes(byte[] data, String key) {
        if (useDES) {
            return desEncrypt(data);
        } else {
            return vigenereEncryptBytes(data, key);
        }
    }

    private byte[] decryptBytes(byte[] data, String key) {
        if (useDES) {
            return desDecrypt(data);
        } else {
            return vigenereDecryptBytes(data, key);
        }
    }

    // Criptografia DES
    private byte[] desEncrypt(byte[] data) {
        try {
            byte[] idBytes = Arrays.copyOfRange(data, 0, 4);
            byte[] toEncrypt = Arrays.copyOfRange(data, 4, data.length);

            Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, desKey);
            byte[] encryptedBody = cipher.doFinal(toEncrypt);

            byte[] result = new byte[4 + encryptedBody.length];
            System.arraycopy(idBytes, 0, result, 0, 4);
            System.arraycopy(encryptedBody, 0, result, 4, encryptedBody.length);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private byte[] desDecrypt(byte[] data) {
        try {
            byte[] idBytes = Arrays.copyOfRange(data, 0, 4);
            byte[] encryptedBody = Arrays.copyOfRange(data, 4, data.length);

            Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, desKey);
            byte[] decryptedBody = cipher.doFinal(encryptedBody);

            byte[] result = new byte[4 + decryptedBody.length];
            System.arraycopy(idBytes, 0, result, 0, 4);
            System.arraycopy(decryptedBody, 0, result, 4, decryptedBody.length);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Criptografia Vigenere
    private byte[] vigenereEncryptBytes(byte[] data, String key) {
        byte[] keyBytes = key.getBytes();
        for (int i = 4; i < data.length; i++) {
            data[i] = (byte)((data[i] + keyBytes[(i-4) % keyBytes.length]) & 0xFF);
        }
        return data;
    }

    private byte[] vigenereDecryptBytes(byte[] data, String key) {
        byte[] keyBytes = key.getBytes();
        for (int i = 4; i < data.length; i++) {
            data[i] = (byte)((data[i] - keyBytes[(i-4) % keyBytes.length] + 256) & 0xFF);
        }
        return data;
    }
}
