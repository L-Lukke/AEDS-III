import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class BalancedMergeSort {
    // Static variables for file name, pointer management, and global settings
    private static final String fileName = "output/raf.bin"; // Main binary file to be processed
    private static final String SUFFIX = ".bin"; // Extension for temporary files
    private static long pointerController = 4; // Controls the pointer position in the file during reading
    private static int entries; // Total number of entries to be processed
    private static String fileNameFinal = ""; // Name of the final sorted output file
    
    // Instance variables for the sort configuration
    private int ram; // RAM memory limit to read chunks of the file
    private int paths; // Number of paths (partitions) to split data
    private int keep; // Determines whether to keep or delete temp files (-1: delete all, 0: keep none)
    private String key; // Sorting key (e.g., "id", "name", etc.)

    // Default constructor
    public BalancedMergeSort() {}

    // Parameterized constructor to initialize key sorting variables
    public BalancedMergeSort(int keep, int ram, int paths, String key){
        this.keep = keep;
        this.ram = ram;
        this.paths = paths;
        this.key = key;
        BalancedMergeSort.entries = fragment(ram, paths, key); // Determine the limit of records to process
    }

    // Main sorting method
    public void sort(){
        boolean isBase = true; // Flag to toggle between temp files for intercalation
        while (ram < entries) {
            merge(ram, paths, isBase, key); // Perform balanced merge
            ram *= paths; // Increase RAM allocation with each iteration
            isBase = !isBase; // Switch between base and temporary files
        }

        // Display sorted data and file details
        System.out.print("Ordered IDs: ");
        listHeroes(fileNameFinal);
        System.out.println("Bin file of ordered entries: " + fileNameFinal);

        // Clean up temporary files based on 'keep' option
        if(keep == 0) deleteTempFiles();
        if(keep == -1) deleteAllFiles();
    }

    // Delete all temp files created during sorting
    public void deleteAllFiles() {
        for (int i = 0; i < paths * 2; i++) {
            File file = new File("output/tmp" + i + SUFFIX);
            if (file.exists()) {
                if (file.delete()) {
                    System.out.println("Temporary file deleted: " + file.getAbsolutePath());
                } else {
                    System.out.println("Failed to delete temporary file: " + file.getAbsolutePath());
                }
            }
        }
    }

    // Delete temp files except the first one
    private void deleteTempFiles() {
        for (int i = 1; i < paths * 2; i++) {
            File file = new File("output/tmp" + i + SUFFIX);
            if (file.exists()) {
                if (file.delete()) {
                    System.out.println("Temporary file deleted: " + file.getAbsolutePath());
                } else {
                    System.out.println("Failed to delete temporary file: " + file.getAbsolutePath());
                }
            }
        }
    }

    // Distribute records into temporary files for sorting
    public static int fragment(int ram, int paths, String key) {
        int quantidade = 0; // Track total number of records
        try {
            List<Hero> heroes = new ArrayList<>(); // Buffer to store heroes before sorting
            RandomAccessFile[] temp = new RandomAccessFile[paths]; // Array for temporary files
            for (int i = 0; i < paths; i++) {
                temp[i] = new RandomAccessFile("output/tmp" + i + SUFFIX, "rw"); // Create temp files
            }
            
            // Loop through the file, splitting and sorting chunks
            while (pointerController != -1) {
                for (int i = 0; i < paths; i++) {
                    for (int j = 0; j < ram; j++) {
                        var hero = readFile(fileName); // Read a chunk of data from file
                        if (hero != null) {
                            heroes.add(hero); // Add to buffer
                        }
                    }

                    // Sort the buffer based on the selected key
                    switch (key) {
                        case "id": heroes.sort(HeroComparator.byId()); break;
                        case "name": heroes.sort(HeroComparator.byName()); break;
                        case "identity": heroes.sort(HeroComparator.byIdentity()); break;
                        case "alignment": heroes.sort(HeroComparator.byAlignment()); break;
                        case "eye color": heroes.sort(HeroComparator.byEyeColor()); break;
                        case "hair color": heroes.sort(HeroComparator.byHairColor()); break;
                        case "gender": heroes.sort(HeroComparator.byGender()); break;
                        case "status": heroes.sort(HeroComparator.byStatus()); break;
                        case "appearances": heroes.sort(HeroComparator.byAppearances()); break;
                        case "first appearance": heroes.sort(HeroComparator.byFirstAppearance()); break;
                        case "universe": heroes.sort(HeroComparator.byUniverse()); break;
                    }

                    // Write sorted buffer to the corresponding temp file
                    for (Hero hero : heroes) {
                        byte[] ba = hero.toByteArray(); // Convert hero to byte array
                        temp[i].writeByte(0); // Write marker (not deleted)
                        temp[i].writeInt(ba.length); // Write record length
                        temp[i].write(ba); // Write record data
                    }
                    quantidade += heroes.size(); // Update total count
                    heroes.clear(); // Clear buffer for next iteration
                }
            }
            // Close all temp files
            for (var t : temp) {
                t.close();
            }
        } catch (Exception e) {
            System.out.println("Erro dist. " + e.getMessage());
            e.printStackTrace();
        }
        return quantidade; // Return total number of records processed
    }

    // Perform balanced merging of sorted temp files
    public static void merge(int ram, int paths, boolean isBase, String key) {
        HeroFile[] temp1 = new HeroFile[paths]; // Temp files for reading
        HeroFile[] temp2 = new HeroFile[paths]; // Temp files for writing
        
        // Initialize temp files based on current phase (base or not)
        for (int i = 0; i < paths; i++) {
            temp1[i] = new HeroFile("output/tmp" + (isBase ? i : i + paths) + SUFFIX);
            temp2[i] = new HeroFile("output/tmp" + (isBase ? i + paths : i) + SUFFIX);
        }

        Map<HeroFile, Hero> map = new HashMap<>(); // Track heroes from temp files
        int tempPos = 0; // Position for writing to temp2
        try {
            while (true) {
                // Read heroes from temp1 files into the map
                for (int i = 0; i < paths; i++) {
                    if (map.get(temp1[i]) == null && temp1[i].heroesReadFromFile < ram) {
                        Hero hero = temp1[i].readHero();
                        if (hero != null) {
                            map.put(temp1[i], hero); // Store hero in map
                        }
                    }
                }
                if (map.isEmpty()) break; // Exit loop if all files are empty

                // Sort heroes based on the selected key
                List<Map.Entry<HeroFile, Hero>> ordered = map.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(HeroComparator.byId()))
                    .collect(Collectors.toList());
                
                switch (key) {
                    case "id": ordered = map.entrySet().stream().sorted(Map.Entry.comparingByValue(HeroComparator.byId())).collect(Collectors.toList()); break;
                    case "name": ordered = map.entrySet().stream().sorted(Map.Entry.comparingByValue(HeroComparator.byName())).collect(Collectors.toList()); break;
                    case "identity": ordered = map.entrySet().stream().sorted(Map.Entry.comparingByValue(HeroComparator.byIdentity())).collect(Collectors.toList()); break;
                    case "alignment": ordered = map.entrySet().stream().sorted(Map.Entry.comparingByValue(HeroComparator.byAlignment())).collect(Collectors.toList()); break ;
                    case "eye color": ordered = map.entrySet().stream().sorted(Map.Entry.comparingByValue(HeroComparator.byEyeColor())).collect(Collectors.toList()); break ;
                    case "hair color": ordered = map.entrySet().stream().sorted(Map.Entry.comparingByValue(HeroComparator.byHairColor())).collect(Collectors.toList()); break ;
                    case "gender": ordered = map.entrySet().stream().sorted(Map.Entry.comparingByValue(HeroComparator.byGender())).collect(Collectors.toList()); break ;
                    case "status": ordered = map.entrySet().stream().sorted(Map.Entry.comparingByValue(HeroComparator.byStatus())).collect(Collectors.toList()); break ;
                    case "appearances": ordered = map.entrySet().stream().sorted(Map.Entry.comparingByValue(HeroComparator.byAppearances())).collect(Collectors.toList()); break ;
                    case "first appearance": ordered = map.entrySet().stream().sorted(Map.Entry.comparingByValue(HeroComparator.byFirstAppearance())).collect(Collectors.toList()); break ;
                    case "universe": ordered = map.entrySet().stream().sorted(Map.Entry.comparingByValue(HeroComparator.byUniverse())).collect(Collectors.toList()); break ;
                }

                // Write the smallest hero to temp2 and update tracking variables
                var firstHero = ordered.get(0);
                temp2[tempPos].writeHero(firstHero.getValue()); // Write hero to temp2
                
                if (temp2[tempPos].numberOfEntries == entries) { // Check if we've reached the final output
                    fileNameFinal = temp2[tempPos].fileName; // Set final output file name
                    break;
                }

                // Rotate to the next temp2 file if necessary
                if (temp2[tempPos].numberOfEntries % (ram * paths) == 0) {
                    tempPos++;
                    if (tempPos == paths) {
                        tempPos = 0;
                    }
                    for (var t : temp1) {
                        t.heroesReadFromFile = 0; // Reset reading counters
                    }
                }
                map.remove(firstHero.getKey()); // Remove the written hero from the map
            }

            // Close all files
            for (var t : temp1) t.file.close();
            for (var t : temp2) t.file.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Read a hero record from the file
    public static Hero readFile(String fileName) {
        byte tombstone;
        int size;
        byte[] ba;
        try (RandomAccessFile file = new RandomAccessFile(fileName, "r")) {
            if (pointerController < file.length() && pointerController != -1) {
                file.seek(pointerController); // Move to the correct file pointer
                tombstone = file.readByte(); // Read marker byte
                size = file.readInt(); // Read size of the record
                ba = new byte[size]; // Create byte array for the record
                file.read(ba); // Read record data
                pointerController = file.getFilePointer(); // Update pointer position
                if (tombstone != '1') return Hero.fromByteArray(ba); // Return hero if not deleted
            } else pointerController = -1; // End of file reached
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null; // Return null if no valid record is found
    }

    // List all heroes from the final sorted file
    public static void listHeroes(String nome) {
        byte[] array;
        byte tombstone;

        try (RandomAccessFile arquivo = new RandomAccessFile(nome, "r")) { // Open file for reading
            while (arquivo.getFilePointer() < arquivo.length()) {
                tombstone = arquivo.readByte(); // Read marker
                int size = arquivo.readInt(); // Read record size
                array = new byte[size]; // Create buffer for record
                arquivo.read(array); // Read record data

                if (tombstone != '1') { // Check if record is not deleted
                    Hero hero = Hero.fromByteArray(array); // Deserialize hero
                    if (hero != null) {
                        System.out.print(hero.getId() + " "); // Print hero ID
                    } else {
                        System.out.println("Error: Unable to deserialize hero from byte array.");
                    }
                }
            }
        System.out.println();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


