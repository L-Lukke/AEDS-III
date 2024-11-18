import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;


public class Db {

    Hash hashIndex;
    BTree btreeIndex;
    FullInvertedIndex fullInvertedIndex;
    private int compressionVersion = 1;

    private int lastId = 0;
    public RandomAccessFile raf;

    // Constructor: Initializes the RandomAccessFile and reads the lastId
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
        } catch (Exception e) {
            e.printStackTrace();
        }
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
    

    // Getter for lastId
    public int getLastId() {
        return lastId;
    }

    // Imports heroes from a CSV file and writes them to the binary file
    public void databaseToBinary(Hero hero, boolean hash, boolean btree) {
        BufferedReader br = null;

        try {
            // Try reading the CSV file with two different encodings
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

            String line = br.readLine(); // Skip the header line of the CSV file
            line = br.readLine(); // Read the first data line

            if (hash) {
                while (line != null) {
                    Hero.read(line, hero); // Parse CSV line to a Hero object
                    createHash(hero); // Write the hero to the binary file and hash index
                    line = br.readLine(); // Read next line
                }
            }

            else if (btree) {
                while (line != null) {
                    Hero.read(line, hero); // Parse CSV line to a Hero object
                    createBTree(hero); // Write the hero to the binary file and btree index
                    line = br.readLine(); // Read next line
                }
            }

            else {
                while (line != null) {
                    Hero.read(line, hero); // Parse CSV line to a Hero object
                    create(hero); // Write the hero to the binary file
                    line = br.readLine(); // Read next line
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
            raf.seek(0); // Reset file pointer
            raf.readInt(); // Skip the lastId

            // Prepare a file to write the matched entries
            try (BufferedWriter writer = new BufferedWriter(new FileWriter("output/stringMatch/matchedHeroes.csv"))) {

                while (raf.getFilePointer() < raf.length()) {
                    byte tombstone = raf.readByte(); // Read the tombstone byte
                    int size = raf.readInt(); // Read the size of the record

                    if (tombstone == 1) { // Skip over the deleted record
                        raf.seek(raf.getFilePointer() + size); // Move the pointer forward by the size of the record
                        continue;
                    }

                    heroByte = new byte[size];
                    raf.read(heroByte); 
                    Hero hero = new Hero();
                    hero = Hero.fromByteArray(heroByte);

                    // Dynamically retrieve the value to search based on the variable
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

                    // Perform the string matching based on the selected algorithm
                    boolean isMatch = false;
                    if (algorithm == 'k') {
                        isMatch = kmpSearch(valueToSearch.toLowerCase(), searchString.toLowerCase());
                    } else if (algorithm == 'b') {
                        isMatch = boyerMooreSearch(valueToSearch.toLowerCase(), searchString.toLowerCase());
                    } else {
                        System.out.println("Invalid algorithm selected. Please try again.");
                        return matchingHeroes;
                    }

                    // If a match is found, add the hero to the results list and write to the file
                    if (isMatch) {
                        matchingHeroes.add(hero);

                        // Write the matched hero to the file
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
            raf.seek(0); // Reset file pointer
            raf.readInt(); // Skip the lastId
    
            while (raf.getFilePointer() < raf.length()) {

                byte tombstone = raf.readByte(); // Read the tombstone byte
                int size = raf.readInt(); // Read the size of the record

                if (tombstone == 1) { // Skip over the deleted record
                    raf.seek(raf.getFilePointer() + size); // Move the pointer forward by the size of the record
                    continue;
                }

                heroByte = new byte[size];
                raf.read(heroByte); 
                Hero hero = new Hero();
                hero = Hero.fromByteArray(heroByte);
    
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

                // Perform the string matching based on the selected algorithm
                boolean isMatch = false;
                if (algorithm == 'k') {
                    isMatch = kmpSearch(valueToSearch.toLowerCase(), searchString.toLowerCase());
                } else if (algorithm == 'b') {
                    isMatch = boyerMooreSearch(valueToSearch.toLowerCase(), searchString.toLowerCase());
                } else {
                    System.out.println("Invalid algorithm selected.");
                    return matchingHeroes;
                }
    
                // If a match is found, add the hero to the results list
                if (isMatch) {
                    matchingHeroes.add(hero);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    
        return matchingHeroes;
    }

    // KMP Algorithm
    private boolean kmpSearch(String text, String pattern) {
        int[] lps = computeLPSArray(pattern);
        int i = 0, j = 0;

        while (i < text.length()) {
            if (pattern.charAt(j) == text.charAt(i)) {
                i++;
                j++;
            }
            if (j == pattern.length()) {
                return true; // Match found
            } else if (i < text.length() && pattern.charAt(j) != text.charAt(i)) {
                if (j != 0) {
                    j = lps[j - 1];
                } else {
                    i++;
                }
            }
        }
        return false; // No match found
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
                if (length != 0) {
                    length = lps[length - 1];
                } else {
                    lps[i] = 0;
                    i++;
                }
            }
        }
        return lps;
    }

    // Boyer-Moore Algorithm with Good Suffix Rule
    private boolean boyerMooreSearch(String text, String pattern) {
        int[] badChar = preprocessBadCharacterRule(pattern);
        int[] goodSuffix = preprocessGoodSuffixRule(pattern);
        int shift = 0;

        while (shift <= text.length() - pattern.length()) {
            int j = pattern.length() - 1;

            // Match from the end of the pattern
            while (j >= 0 && pattern.charAt(j) == text.charAt(shift + j)) {
                j--;
            }

            if (j < 0) {
                // Match found
                return true;
            } else {
                // Use both bad character and good suffix rules to determine the shift
                int badCharShift = j - badChar[text.charAt(shift + j)];
                int goodSuffixShift = goodSuffix[j];
                shift += Math.max(1, Math.max(badCharShift, goodSuffixShift));
            }
        }
        return false; // No match found
    }

    // Preprocessing for the Bad Character Rule
    private int[] preprocessBadCharacterRule(String pattern) {
        int[] badChar = new int[256]; // Assuming extended ASCII characters
        Arrays.fill(badChar, -1);

        for (int i = 0; i < pattern.length(); i++) {
            badChar[pattern.charAt(i)] = i;
        }
        return badChar;
    }

    // Preprocessing for the Good Suffix Rule
    private int[] preprocessGoodSuffixRule(String pattern) {
        int m = pattern.length();
        int[] goodSuffix = new int[m];
        int[] borderPos = new int[m + 1]; // Temporary array to store border positions

        // Initialize border positions
        int i = m; // Start at the last character of the pattern
        int j = m + 1; // Set j to one beyond the last character
        borderPos[i] = j;

        // Build the borderPos array
        while (i > 0) {
            // While mismatch occurs, update goodSuffix for previous mismatched positions
            while (j <= m && pattern.charAt(i - 1) != pattern.charAt(j - 1)) {
                if (goodSuffix[j - 1] == 0) {
                    goodSuffix[j - 1] = j - i; // Distance to shift
                }
                j = borderPos[j]; // Fall back to the next border position
            }
            i--;
            j--;
            borderPos[i] = j; // Update border position for the current index
        }

        // Fill the goodSuffix array for patterns without proper suffix matches
        j = borderPos[0]; // Start with the border of the entire pattern
        for (i = 0; i < m; i++) {
            if (goodSuffix[i] == 0) {
                goodSuffix[i] = j; // If no proper suffix, use the border position
            }
            if (i + 1 == j) { // Update j to the next border position
                j = borderPos[j];
            }
        }

        return goodSuffix;
    }

    // Hash methods

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

            long recordAddress = raf.length();
            raf.seek(recordAddress);
            raf.writeByte(0); // tombstone = 0 (not deleted)
            raf.writeInt(ba.length); // write size of record
            raf.write(ba); // write the hero data

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
                    return null; // Record has been deleted
                }

                int size = raf.readInt();
                byte[] heroByte = new byte[size];
                raf.read(heroByte);
                return Hero.fromByteArray(heroByte);
            }

            return null; // Hero not found

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
                    return false; // Record has been deleted
                }

                int oldSize = raf.readInt();
                byte[] updatedBytes = updatedHero.toByteArray();

                if (updatedBytes.length <= oldSize) {
                    // New hero data fits in the same space, overwrite it
                    raf.seek(address + 1 + 4); // Skip tombstone byte and size
                    raf.write(updatedBytes);
                } else {
                    // New record is larger, mark the old one as deleted and append new record
                    raf.seek(address);
                    raf.writeByte(1); // Mark as deleted

                    // Append the new record at the end of the file
                    long newAddress = raf.length();
                    raf.seek(newAddress);
                    raf.writeByte(0); // New tombstone = 0 (not deleted)
                    raf.writeInt(updatedBytes.length);
                    raf.write(updatedBytes);

                    // Update the hash index with the new address
                    hashIndex.removeFromHashIndex(id);
                    hashIndex.addToHashIndex(id, newAddress);
                }

                return true;
            }

            return false; // Hero not found

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

    // BTree methods

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

            long recordAddress = raf.length();
            raf.seek(recordAddress);
            raf.writeByte(0); // tombstone = 0 (not deleted)
            raf.writeInt(ba.length); // write size of record
            raf.write(ba); // write the hero data

            btreeIndex.insert(hero.getId(), recordAddress); // Add to the BTree

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Hero readBTree(int id) {
        try {
            long address = btreeIndex.search(id); // Search in BTree

            if (address != -1) {
                raf.seek(address);
                byte tombstone = raf.readByte();

                if (tombstone == 1) {
                    return null; // Record has been deleted
                }

                int size = raf.readInt();
                byte[] heroByte = new byte[size];
                raf.read(heroByte);
                return Hero.fromByteArray(heroByte); // Convert bytes back to Hero object
            }

            return null; // Hero not found

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean updateBTree(int id, Hero updatedHero) {
        try {
            long address = btreeIndex.search(id); // Search in BTree

            if (address != -1) {
                raf.seek(address);
                byte tombstone = raf.readByte();

                if (tombstone == 1) {
                    return false; // Record has been deleted
                }

                int oldSize = raf.readInt();
                byte[] updatedBytes = updatedHero.toByteArray();

                if (updatedBytes.length <= oldSize) {
                    // New hero data fits in the same space, overwrite it
                    raf.seek(address + 1 + 4); // Skip tombstone byte and size
                    raf.write(updatedBytes);
                } else {
                    // New record is larger, mark the old one as deleted and append new record
                    raf.seek(address);
                    raf.writeByte(1); // Mark as deleted

                    // Append the new record at the end of the file
                    long newAddress = raf.length();
                    raf.seek(newAddress);
                    raf.writeByte(0); // New tombstone = 0 (not deleted)
                    raf.writeInt(updatedBytes.length);
                    raf.write(updatedBytes);

                    // Update the BTree with the new address
                    btreeIndex.insert(id, newAddress); // Insert new address into BTree
                }

                return true;
            }

            return false; // Hero not found

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteBTree(int id) {
        try {
            
            btreeIndex.remove(id); // Remove from BTree
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void printBtreeState() {
        btreeIndex.printState();
    }


    // Sequential methods (DEPRECATED)

    // Creates a new hero entry in the binary file
    public boolean create(Hero hero) {
        try {
            // If the file is empty (i.e., length is 0 after massDelete), initialize lastId to 1
            if (raf.length() == 0) {
                lastId = 1;
                raf.seek(0);
                raf.writeInt(lastId); // Write the initial lastId at the start of the file
            } else {
                // Read the existing lastId from the file if the file is not empty
                raf.seek(0);
                lastId = raf.readInt();
                lastId++; // Increment the lastId
                raf.seek(0);
                raf.writeInt(lastId); // Update lastId in the file
            }

            // Convert hero to byte array
            byte[] ba = hero.toByteArray();

            // Write the hero entry to the file
            raf.seek(raf.length()); // Go to the end of the file
            raf.writeByte(0); // tombstone = 0 (not deleted)
            raf.writeInt(ba.length); // write size of record
            raf.write(ba); // write the hero data

            fullInvertedIndex.add(hero);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Reads a hero record by ID
    public Hero read(int id) {
        try {
            raf.seek(4); // Start after the initial lastId int (4 bytes)
            int size;
            byte[] heroByte;
            Hero hero;

            while (raf.getFilePointer() < raf.length()) {
                byte tombstone = raf.readByte(); // Read the tombstone byte
                size = raf.readInt(); // Read the size of the record

                if (tombstone == 1) { // Skip over the deleted record
                    raf.seek(raf.getFilePointer() + size); // Move the pointer forward by the size of the record
                    continue;
                }

                heroByte = new byte[size];
                raf.read(heroByte); 
                hero = new Hero();
                hero = Hero.fromByteArray(heroByte);

                if (hero.getId() == id) {
                    return hero; // Return the hero if the ID matches
                }
            }

            return null; // If no hero with the given id was found

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }    

    // Updates a hero record by ID
    public boolean update(int id, Hero updatedHero) {
        try {
            raf.seek(4); // Start after the initial lastId int (4 bytes)
            int size;
            byte[] heroByte;
            Hero hero;
        
            while (raf.getFilePointer() < raf.length()) {
                long recordPosition = raf.getFilePointer(); // Save position before reading the tombstone
                byte tombstone = raf.readByte(); // Read the tombstone byte
                size = raf.readInt(); // Read the size of the record
        
                if (tombstone == 1) { // Skip over the deleted record
                    raf.seek(raf.getFilePointer() + size);
                    continue;
                }
        
                heroByte = new byte[size];
                raf.read(heroByte); 
                hero = new Hero();
                hero = Hero.fromByteArray(heroByte);

                Hero oldHero = read(id);
                if (oldHero == null) {
                    return false;
                }

                fullInvertedIndex.update(oldHero, updatedHero);
        
                if (hero.getId() == id) {
                    byte[] updatedBytes = updatedHero.toByteArray();

                    if (updatedBytes.length <= size) {
                        // New hero data fits in the same size, overwrite it
                        raf.seek(recordPosition + 1 + 4); // Skip tombstone byte and size
                        raf.write(updatedBytes); // Overwrite with the updated hero data
                    } else {
                        // New record is larger, mark the old one as deleted
                        raf.seek(recordPosition); // Go back to the start of the record
                        raf.writeByte(1); // Mark the tombstone byte as 1 (deleted)
                        
                        // Append the new record at the end of the file
                        raf.seek(raf.length()); // Go to the end of the file
                        raf.writeByte(0); // New tombstone = 0 (not deleted)
                        raf.writeInt(updatedBytes.length); // Write size of the new record
                        raf.write(updatedBytes); // Write the new hero data
                    }
                    return true; // Update was successful
                }
            }
        
            return false; // No record found with the given ID
        
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public void reinitialize() {
        try {
            // Close existing RandomAccessFile
            if (raf != null) {
                raf.close();
            }
    
            // Recreate the raf.bin file
            raf = new RandomAccessFile("output/raf.bin", "rw");
            lastId = 0;
            raf.writeInt(lastId);
    
            // Reinitialize other indexes if necessary
            hashIndex = new Hash("output/hash/hashIndex.csv");
            btreeIndex = new BTree("output/btree/btreeIndex.csv");
            fullInvertedIndex = new FullInvertedIndex("output/inverted/invertedIndex.csv");
    
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    

    // Marks a hero record as deleted by ID
    public boolean delete(int id) {
        try {
            raf.seek(4); // Start after the initial lastId int
            int size;
            byte[] heroByte;
            Hero hero;

            while (raf.getFilePointer() < raf.length()) {
                long recordPosition = raf.getFilePointer(); // Save position before reading the tombstone
                byte tombstone = raf.readByte();
                size = raf.readInt();

                if (tombstone == 1) { // Skip over the deleted record
                    raf.seek(raf.getFilePointer() + size);
                    continue;
                }

                heroByte = new byte[size];
                raf.read(heroByte);
                hero = new Hero();
                hero = Hero.fromByteArray(heroByte);

                if (hero.getId() == id) {
                    raf.seek(recordPosition); // Go back to the start of the record
                    raf.writeByte(1); // Mark the tombstone byte as 1 (deleted)
                    return true; // Record deleted
                }
            }

            hero = read(id);
            fullInvertedIndex.remove(hero);

            return false; // No record found with the given id

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    public List<Hero> searchHeroes(String[] keywords) {
        List<Set<Integer>> keywordIdSets = new ArrayList<>();

        // Para cada palavra-chave, obtenha o conjunto de IDs correspondentes
        for (String keyword : keywords) {
            List<Integer> ids = fullInvertedIndex.search(keyword.toLowerCase());
            Set<Integer> idSet = new HashSet<>(ids);
            keywordIdSets.add(idSet);
        }

        // Realize a interseção dos conjuntos de IDs
        Set<Integer> resultIds;
        if (!keywordIdSets.isEmpty()) {
            resultIds = keywordIdSets.get(0);
            for (int i = 1; i < keywordIdSets.size(); i++) {
                resultIds.retainAll(keywordIdSets.get(i));
            }
        } else {
            resultIds = new HashSet<>();
        }

        // Carregar os heróis correspondentes aos IDs resultantes
        List<Hero> results = new ArrayList<>();
        for (int id : resultIds) {
            Hero hero = read(id); // Usar o método apropriado (read, readHash, etc.)
            if (hero != null) {
                results.add(hero);
            }
        }

        return results;
    }

    // General methods

    public boolean massDelete() {
        try {
            raf.setLength(0);
            lastId = 0;
            hashIndex = new Hash("output/hash/hashIndex.csv"); // Reinitialize the hash index
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Compression and Decompression

    public void createDirectories() {

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
    
    

    public void compressDatabase() {
        createDirectories();
    
        String inputFileName = "output/raf.bin";
    
        try {
            // Huffman Compression
            Huffman huffman = new Huffman();
            String huffmanOutputFileName = "output/compressed/raf.binHuffmanCompression" + compressionVersion;
    
            long huffmanStartTime = System.currentTimeMillis();
            huffman.compress(inputFileName, huffmanOutputFileName);
            long huffmanEndTime = System.currentTimeMillis();
            long huffmanTime = huffmanEndTime - huffmanStartTime;
    
            // Compute sizes
            File inputFile = new File(inputFileName);
            File huffmanFile = new File(huffmanOutputFileName);
            long inputFileSize = inputFile.length();
            long huffmanFileSize = huffmanFile.length();
    
            double huffmanCompressionRatio = ((double) (inputFileSize - huffmanFileSize) / inputFileSize) * 100;
    
            // LZW Compression
            LZW lzw = new LZW();
            String lzwOutputFileName = "output/compressed/raf.binLZWCompression" + compressionVersion;
    
            long lzwStartTime = System.currentTimeMillis();
            lzw.compress(inputFileName, lzwOutputFileName);
            long lzwEndTime = System.currentTimeMillis();
            long lzwTime = lzwEndTime - lzwStartTime;
    
            // Compute sizes
            File lzwFile = new File(lzwOutputFileName);
            long lzwFileSize = lzwFile.length();
    
            double lzwCompressionRatio = ((double) (inputFileSize - lzwFileSize) / inputFileSize) * 100;
    
            // Display results
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
    
            // Increment the version number
            compressionVersion++;
    
        } catch (Exception e) {
            e.printStackTrace();
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
    

    public void decompressDatabase(int version) {
        

        String huffmanInputFileName = "output/compressed/raf.binHuffmanCompression" + version;
        String lzwInputFileName = "output/compressed/raf.binLZWCompression" + version;
        String outputFileNameHuffman = "output/decompressed/raf_decompressed_huffman.bin";
        String outputFileNameLZW = "output/decompressed/raf_decompressed_lzw.bin";

        try {
            // Huffman Decompression
            Huffman huffman = new Huffman();

            long huffmanStartTime = System.currentTimeMillis();
            huffman.decompress(huffmanInputFileName, outputFileNameHuffman);
            long huffmanEndTime = System.currentTimeMillis();
            long huffmanTime = huffmanEndTime - huffmanStartTime;

            // LZW Decompression
            LZW lzw = new LZW();

            long lzwStartTime = System.currentTimeMillis();
            lzw.decompress(lzwInputFileName, outputFileNameLZW);
            long lzwEndTime = System.currentTimeMillis();
            long lzwTime = lzwEndTime - lzwStartTime;

            // Display results
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

            // Replace the database file with the decompressed data
            File rafFile = new File("output/raf.bin");
            if (huffmanTime <= lzwTime) {
                // Use the decompressed file from Huffman
                Files.copy(Paths.get(outputFileNameHuffman), rafFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Database has been replaced with the decompressed data from Huffman.");
            } else {
                // Use the decompressed file from LZW
                Files.copy(Paths.get(outputFileNameLZW), rafFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Database has been replaced with the decompressed data from LZW.");
            }

            reopen();

        } catch (FileNotFoundException e) {
            System.out.println("Compressed file not found for the given version in the 'output/compressed/' directory.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}