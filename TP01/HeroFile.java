import java.io.IOException;
import java.io.RandomAccessFile;

public class HeroFile {
    public RandomAccessFile file;
    public int numberOfEntries;
    public int heroesReadFromFile;
    public String fileName;

    // Constructor
    public HeroFile(String path) {
        try {
            fileName = path;
            file = new RandomAccessFile(path, "rw");
            
            // Initialize the size and readRegisterSize counters to zero
            numberOfEntries = 0;
            heroesReadFromFile = 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Method to read the next hero record from the file
    public Hero readHero() {
        byte[] ba; // Byte array to hold the hero data
        try {
            // Ensure that the file pointer is not at the end of the file before attempting to read
            if (file.getFilePointer() < file.length()) {
                file.readByte(); // Read the tombstone
                ba = new byte[file.readInt()]; // Read the length of the hero's data
                file.read(ba); // Read the hero's data into the byte array
                heroesReadFromFile++;
                
                return Hero.fromByteArray(ba);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // Method to write a hero record to the file
    public void writeHero(Hero hero) throws IOException {
        byte[] ba = hero.toByteArray();
        file.writeByte(0); // Write a valid tombstone marker
        file.writeInt(ba.length); // Write the length of the hero's byte data
        file.write(ba); // Write the actual byte data of the hero to the file
        numberOfEntries++;
    }
}
