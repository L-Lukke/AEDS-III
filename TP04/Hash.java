import java.io.*;
import java.util.*;

public class Hash {
    private static final int BUCKET_SIZE = 5;
    private int globalDepth;
    private List<Bucket> directory;
    private final String indexFilePath;

    public Hash(String indexFilePath) {
        this.indexFilePath = indexFilePath;
        this.globalDepth = 0;
        this.directory = new ArrayList<>(1 << globalDepth);
        directory.add(new Bucket(0));
        readHashIndexFromFile();
    }

    private static class Bucket {
        int localDepth;
        List<Entry> entries;

        Bucket(int depth) {
            this.localDepth = depth;
            this.entries = new ArrayList<>();
        }
    }

    private static class Entry {
        int key;
        long address;

        Entry(int key, long address) {
            this.key = key;
            this.address = address;
        }
    }

    void printState() {
        System.out.println();
        System.out.println("Global Depth: " + globalDepth);
        for (int i = 0; i < directory.size(); i++) {
            Bucket bucket = directory.get(i);
            System.out.print("Bucket " + i + " (Depth " + bucket.localDepth + "): ");
            for (Entry entry : bucket.entries) {
                System.out.print("[" + entry.key + ":" + entry.address + "] ");
            }
            System.out.println();
        }
        System.out.println();
    }
    
    public void addToHashIndex(int key, long address) {
        int bucketIndex = key & ((1 << globalDepth) - 1);
        Bucket targetBucket = directory.get(bucketIndex);

        // System.out.println("Global Depth: " + globalDepth);
        // System.out.println("Local Depth of Target Bucket: " + targetBucket.localDepth);

    
        // Debug: Show which bucket is being targeted for addition
        // System.out.println("Adding Key: " + key + " to Bucket Index: " + bucketIndex);
    
        if (targetBucket.entries.size() < BUCKET_SIZE) {
            targetBucket.entries.add(new Entry(key, address));
            // System.out.println("Added Key: " + key + " to Bucket Index: " + bucketIndex);
        } else {
            splitBucket(bucketIndex);
            addToHashIndex(key, address); // Retry adding to the hash index
        }
        writeHashIndexToFile();
    }  

    private void splitBucket(int bucketIndex) {
        Bucket oldBucket = directory.get(bucketIndex);
        
        // Increase global depth if needed
        if (oldBucket.localDepth == globalDepth) {
            doubleDirectory();
        }
    
        int newLocalDepth = oldBucket.localDepth + 1;
        Bucket newBucket = new Bucket(newLocalDepth);
    
        // Create a mask to differentiate between old and new bucket entries
        int localMask = 1 << oldBucket.localDepth;
        
        // Redistribute entries between old and new buckets
        Iterator<Entry> iterator = oldBucket.entries.iterator();
        while (iterator.hasNext()) {
            Entry entry = iterator.next();
            if ((entry.key & localMask) != 0) {
                newBucket.entries.add(entry);
                iterator.remove(); // Remove from old bucket after moving to new bucket
            }
        }
    
        oldBucket.localDepth = newLocalDepth;
    
        // Update directory to point to the new bucket for relevant entries
        for (int i = 0; i < directory.size(); i++) {
            if ((i & localMask) != 0 && directory.get(i) == oldBucket) {
                directory.set(i, newBucket);
            }
        }
    }    

    private void doubleDirectory() {
        int oldSize = directory.size();
        for (int i = 0; i < oldSize; i++) directory.add(directory.get(i)); // Duplicate the references 
        globalDepth++;
    }
    
    public long searchTokenInHashIndex(int key) {
        // Calculate the bucket index based on the total number of buckets
        System.out.println();
        int bucketIndex = key % (1 << globalDepth);
        
        // Print the bucket index for the given key
        System.out.println("Searching for Key: " + key + " in Bucket Index: " + bucketIndex);
        
        Bucket bucket = directory.get(bucketIndex);
        
        // Print the contents of the bucket
        System.out.print("Contents of Bucket " + bucketIndex + ": ");
        for (Entry entry : bucket.entries) System.out.print("[" + entry.key + ":" + entry.address + "] ");
        System.out.println(); // New line after bucket contents
        System.out.println();    

        for (Entry entry : bucket.entries) {
            if (entry.key == key) {
                return entry.address;
            }
        }
        return -1; // Not found
    }   

    public synchronized void removeFromHashIndex(int key) {
        int bucketIndex = key % (1 << globalDepth);
        Bucket bucket = directory.get(bucketIndex);
        if (bucket != null) {
            bucket.entries.removeIf(entry -> entry.key == key);
            writeHashIndexToFile();
        }
    }

    private synchronized void writeHashIndexToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(indexFilePath))) {
            writer.write("Bucket\n");
            for (Bucket bucket : directory) {
                if (!bucket.entries.isEmpty()) {
                    StringBuilder line = new StringBuilder();
                    for (Entry entry : bucket.entries) line.append(entry.key).append(" ").append(entry.address).append(",");
                    // Remove the trailing comma
                    writer.write(line.substring(0, line.length() - 1) + "\n");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readHashIndexFromFile() {
        File file = new File(indexFilePath);
        if (!file.exists())  return; // If file doesn't exist, start with an empty index

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            reader.readLine(); // Skip header
            while ((line = reader.readLine()) != null) {
                String[] pairs = line.split(",");
                for (String pair : pairs) {
                    String[] parts = pair.split(" ");
                    int key = Integer.parseInt(parts[0]);
                    long address = Long.parseLong(parts[1]);
                    addToHashIndex(key, address);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
}