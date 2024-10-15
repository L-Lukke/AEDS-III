import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;

public class Db {

    Hash hashIndex;
    BTree btreeIndex;
    FullInvertedIndex fullInvertedIndex;

    private int lastId = 0; // Tracks the last used ID
    public RandomAccessFile raf; // RandomAccessFile for file operations

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
}