import java.io.RandomAccessFile;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;

public class Db {

    private int lastId = 0; // Tracks the last used ID
    public RandomAccessFile raf; // RandomAccessFile for file operations

    // Constructor: Initializes the RandomAccessFile and reads the lastId
    public Db() {
        try {
            this.raf = new RandomAccessFile("output/raf.bin", "rw");
            if (raf.length() != 0) {
                raf.seek(0);
                lastId = raf.readInt(); // Read the lastId from the start of the file
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Getter for lastId
    public int getLastId() {
        return lastId;
    }

    // Imports heroes from a CSV file and writes them to the binary file
    public void databaseToBinary(Hero hero) {
        BufferedReader br = null;

        try {
            // Try reading the CSV file with two different encodings
            try {
                br = new BufferedReader(new InputStreamReader(new FileInputStream("Heroes.csv"), "iso-8859-1"));
            } catch (Exception e) {
                try {
                    br = new BufferedReader(new InputStreamReader(new FileInputStream("Heroes.csv"), "utf-8"));
                } catch (Exception e2) {
                    e.printStackTrace();
                }
            }

            System.out.println();
            System.out.println("Importing DB. This may take a while.");

            String line = br.readLine(); // Skip the header line of the CSV file
            line = br.readLine(); // Read the first data line
            while (line != null) {
                Hero.read(line, hero); // Parse CSV line to a Hero object
                create(hero); // Write the hero to the binary file
                line = br.readLine(); // Read next line
            }

            System.out.println("DB imported successfully");
            System.out.println();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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
    
            return false; // No record found with the given id
    
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }  

    // Clears all records from the binary file
    public boolean massDelete() {
        try {
            raf.setLength(0); // Set the file to 0 bytes, effectively soft-deleting all data in it
            lastId = 0; // Reset lastId to 0 as well
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}