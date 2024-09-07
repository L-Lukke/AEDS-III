import java.io.RandomAccessFile;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class TP01 {
    public static void main(String[] args) throws Exception {
        // Create a Hero object to hold the hero's information for operations
        Hero token = new Hero();
        
        // Scanner for reading user input
        Scanner sc = new Scanner(System.in);
        
        // Variable to store user input
        String line = "";
        
        // RandomAccessFile for binary data storage, initialized to 'rw' (read/write) mode
        RandomAccessFile raf = new RandomAccessFile("output/raf.bin", "rw");
        
        // Write an initial ID of 0 to the file, creating a basic structure for the database
        raf.writeInt(0);
        
        // Create Db object to manage database operations (CRUD)
        Db db = new Db();
        
        // Reset file pointer to the start of the file
        raf.seek(0);

        // Create a BalancedMergeSort object to handle external merge sorting
        BalancedMergeSort bms = new BalancedMergeSort();

        // Infinite loop to display the menu and process user inputs
        while (true) {
            // Display the main menu to the user
            System.out.println("Type S to stop the program.");
            System.out.println("Type I to import and X to sort the database.");
            System.out.println("Type C to create, R to read, U to update, D to delete.");
            System.out.print("> ");
            
            // Read and sanitize user input (convert to lowercase and trim spaces)
            line = sc.nextLine().toLowerCase().trim();
            
            // Option to import data from CSV into the database
            if (line.charAt(0) == 'i') db.databaseToBinary(token);

            // CREATE new hero entry
            if (line.charAt(0) == 'c') {
                System.out.println();
                System.out.println("-- Create Hero --");
            
                // Set a new ID based on the last existing ID in the database
                if (db.getLastId() == 0) {
                    token.setId(1); // Start at ID 1 if the database is empty
                } else {
                    token.setId(db.getLastId() + 1); // Increment the last known ID
                }

                // Get hero's name
                System.out.print("Hero name: ");
                token.setName(sc.nextLine().trim() + " *");

                // Get secret identity status
                System.out.print("Secret Identity? (S for secret, P for public, N for no dual): ");
                line = sc.nextLine().toLowerCase().trim();                
                if (line.charAt(0) == 's') {
                    token.setIdentity("Secret");
                } else if (line.charAt(0) == 'p') {
                    token.setIdentity("Public");
                } else {
                    token.setIdentity("No Dual");
                }

                // Get alignment
                System.out.print("Alignment (G for good, B for bad, N for neutral): ");
                line = sc.nextLine().toLowerCase().trim();
                if (line.charAt(0) == 'g') {
                    token.setAlignment("Good");
                } else if (line.charAt(0) == 'b') {
                    token.setAlignment("Bad");
                } else {
                    token.setAlignment("Neutral");
                }

                // Get eye color
                System.out.print("Eye color (Type 'No' if not applicable): ");
                token.setEyeColor(sc.nextLine().trim());
                
                // Get hair color
                System.out.print("Hair color (Type 'No' if not applicable): ");
                token.setHairColor(sc.nextLine().trim());
                
                // Get gender
                System.out.print("Gender (M for male, F for female, N for not applicable): ");
                line = sc.nextLine().toLowerCase().trim();
                if (line.charAt(0) == 'm') {
                    token.setGender("Male");
                } else if (line.charAt(0) == 'f') {
                    token.setGender("Female");
                } else {
                    token.setGender("Not applicable");
                }
                
                // Get living status
                System.out.print("Living status (L for living, D for deceased): ");
                line = sc.nextLine().toLowerCase().trim();
                if (line.charAt(0) == 'l') {
                    token.setStatus("Living");
                } else {
                    token.setStatus("Deceased");
                }

                // Get number of appearances
                System.out.print("Number of appearances: ");
                token.setAppearances(Integer.parseInt(sc.nextLine().trim()));

                // Get the date of the first appearance and parse it into a LocalDate object
                System.out.print("Date of first appearance (write in dd/mm/yyyy format): ");
                String aux = sc.nextLine().trim();
                token.setFirstAppearance(Hero.stringToDate(aux));

                // Parse the year from the date of the first appearance
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                LocalDate date = LocalDate.parse(aux, formatter);
                token.setYear(date.getYear());

                // Get the universe of the hero
                System.out.print("Universe (M for marvel, D for DC, C for custom, O for others): ");
                line = sc.nextLine().toLowerCase().trim();
                if (line.charAt(0) == 'm') {
                    token.setUniverse("Marvel");
                } else if (line.charAt(0) == 'd') {
                    token.setUniverse("DC");
                } else if (line.charAt(0) == 'c') {
                    token.setUniverse("Custom");
                } else {
                    token.setUniverse("Other");
                }

                // Attempt to create and store the hero in the database
                if (db.create(token)) {
                    System.out.println("-- Hero with ID " + token.getId() + " was created --");
                    System.out.println();
                } else {
                    System.out.println("-- ERROR --");
                    System.out.println();
                }
            }

            // READ hero entry by ID
            else if (line.charAt(0) == 'r') {
                System.out.println();
                System.out.println("-- Read Hero --");
                System.out.print("Type in the ID of the hero you want to read: ");
                line = sc.nextLine().trim();
                int id = Integer.parseInt(line);
                token = db.read(id);

                // Print the hero's details if found, otherwise show an error
                if (token != null) {
                    System.out.println(token.toString());
                    System.out.println();
                } else {
                    System.out.println("-- No hero found --");
                    System.out.println();
                }
            }

            // UPDATE hero entry by ID
            else if (line.charAt(0) == 'u') {
                System.out.println();
                System.out.println("-- Update Hero --");
                System.out.print("Type in the ID of the hero you want to update: ");
                boolean changed = false;
                line = sc.nextLine().trim();
                int id = Integer.parseInt(line);
            
                Hero updatedHero = db.read(id);
                if (updatedHero == null) {
                    System.out.println("No hero found");
                    System.out.println();
                } else {
                    // Ask for confirmation before updating
                    System.out.println(updatedHero.toString());
                    System.out.print("Are you sure you want to update this hero? Y/N "); 
                    if (sc.nextLine().toLowerCase().trim().charAt(0) == 'y') {
                        // Ask the user which attributes they want to change
                        System.out.print("Type what characteristics you desire to change (e.g. 'name, identity, status'): ");
                        String toChange = sc.nextLine().toLowerCase();
                
                        // Update each characteristic based on user input
                        if (toChange.contains("name")) {
                            changed = true;
                            System.out.print("New name: ");
                            line = sc.nextLine().trim();
                            updatedHero.setName(line);
                        }
                
                        if (toChange.contains("identity")) {
                            changed = true;
                            System.out.print("New Secret Identity (S for secret, P for public, N for no dual): ");
                            line = sc.nextLine().toLowerCase().trim();
                            if (line.charAt(0) == 's') {
                                updatedHero.setIdentity("Secret");
                            } else if (line.charAt(0) == 'p') {
                                updatedHero.setIdentity("Public");
                            } else {
                                updatedHero.setIdentity("No Dual");
                            }
                        }
                
                        if (toChange.contains("alignment")) {
                            changed = true;
                            System.out.print("New alignment (G for good, B for bad, N for neutral): ");
                            line = sc.nextLine().toLowerCase().trim();
                            if (line.charAt(0) == 'g') {
                                updatedHero.setAlignment("Good");
                            } else if (line.charAt(0) == 'b') {
                                updatedHero.setAlignment("Bad");
                            } else {
                                updatedHero.setAlignment("Neutral");
                            }
                        }
                
                        if (toChange.contains("eye color")) {
                            changed = true;
                            System.out.print("New eye color (Type 'No' if not applicable): ");
                            line = sc.nextLine().trim();
                            updatedHero.setEyeColor(line);
                        }
                
                        if (toChange.contains("hair color")) {
                            changed = true;
                            System.out.print("New hair color (Type 'No' if not applicable): ");
                            line = sc.nextLine().trim();
                            updatedHero.setHairColor(line);
                        }
                
                        if (toChange.contains("gender")) {
                            changed = true;
                            System.out.print("New gender (M for male, F for female, N for not applicable): ");
                            line = sc.nextLine().toLowerCase().trim();
                            if (line.charAt(0) == 'm') {
                                updatedHero.setGender("Male");
                            } else if (line.charAt(0) == 'f') {
                                updatedHero.setGender("Female");
                            } else {
                                updatedHero.setGender("Not applicable");
                            }
                        }
                
                        if (toChange.contains("status")) {
                            changed = true;
                            System.out.print("New living status (L for living, D for deceased): ");
                            line = sc.nextLine().toLowerCase().trim();
                            if (line.charAt(0) == 'l') {
                                updatedHero.setStatus("Living");
                            } else {
                                updatedHero.setStatus("Deceased");
                            }
                        }
                
                        if (toChange.contains("appearances")) {
                            changed = true;
                            System.out.print("New number of appearances: ");
                            line = sc.nextLine().trim();
                            updatedHero.setAppearances(Integer.parseInt(line));
                        }
                
                        if (toChange.contains("first appearance")) {
                            changed = true;
                            System.out.print("New date of first appearance (write in dd/mm/yyyy format): ");
                            String aux = sc.nextLine().trim();
                            updatedHero.setFirstAppearance(Hero.stringToDate(aux));
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                            LocalDate date = LocalDate.parse(aux, formatter);
                            updatedHero.setYear(date.getYear());
                        }
                
                        if (toChange.contains("universe")) {
                            changed = true;
                            System.out.print("New universe (M for marvel, D for DC, C for custom, O for others): ");
                            line = sc.nextLine().toLowerCase().trim();
                            if (line.charAt(0) == 'm') {
                                updatedHero.setUniverse("Marvel");
                            } else if (line.charAt(0) == 'd') {
                                updatedHero.setUniverse("DC");
                            } else if (line.charAt(0) == 'c') {
                                updatedHero.setUniverse("Custom");
                            } else {
                                updatedHero.setUniverse("Other");
                            }
                        }

                        // After all updates, write changes back to the database
                        if (changed && db.update(id, updatedHero)) {
                            System.out.println("-- Hero with ID " + updatedHero.getId() + " was updated --");
                            System.out.println();
                        } else {
                            System.out.println("-- ERROR --");
                            System.out.println();
                        }
                    }
                }
            }

            // DELETE hero entry by ID
            else if (line.charAt(0) == 'd') {
                boolean deleteAll = false;
                System.out.println();
                System.out.println("-- Delete Hero --");
                System.out.print("Type in the ID of the hero you want to delete (type A to delete all files): ");
                line = sc.nextLine().toLowerCase().trim();

                if(line.charAt(0) == 'a') {
                    deleteAll = true;
                    if(EraseFiles.eraseAllFilesInDirectory("output/")) System.out.println("-- Database dropped successfully --");
                    else System.out.println("-- Couldn't delete files --");
                    System.out.println();
                }
                
                if(deleteAll == false) {
                    int id = Integer.parseInt(line);

                    // Confirm deletion before proceeding
                    Hero toDelete = db.read(id);
                    if (toDelete != null) {
                        System.out.println(toDelete.toString());
                        System.out.print("Are you sure you want to delete this hero? Y/N ");
                        if (sc.nextLine().toLowerCase().trim().charAt(0) == 'y') {
                            if (db.delete(id)) {
                                System.out.println("-- Hero with ID " + id + " was deleted --");
                                System.out.println();
                            } else {
                                System.out.println("-- ERROR --");
                                System.out.println();
                            }
                        }
                    } else {
                        System.out.println("-- No hero found --");
                        System.out.println();
                    }
                }
            }

            // BalancedMergeSort database using external balanced merge sort
            else if (line.charAt(0) == 'x') {
                int paths = 0;
                int ram = 0;
                String key = null;
                boolean error = false;
                boolean cancel = false;

                System.out.println();
                System.out.println("-- Balanced Merge Sort --");
                
                do {
                    error = false;
                    System.out.print("Type what key would you like to sort by (e.g. 'id', 'name', 'identity'): ");
                    line = sc.nextLine().toLowerCase().trim();
                    if (line.equals("id")) key = "id";
                    else if (line.equals("name")) key = "name";
                    else if (line.equals("identity")) key = "identity";
                    else if (line.equals("alignment")) key = "alignment";
                    else if (line.equals("eye color")) key = "eye color";
                    else if (line.equals("hair color")) key = "hair color";
                    else if (line.equals("gender")) key = "gender";
                    else if (line.equals("status")) key = "status";
                    else if (line.equals("appearances")) key = "appearances";
                    else if (line.equals("first appearance")) key = "first appearance";
                    else if (line.equals("universe")) key = "universe";
                    else if (line.charAt(0) == 'c') cancel = true;
                    else {
                        System.out.println("Please, enter a valid sorting key or C to cancel the operation.");
                        error = true; // User did not input any valid sorting key
                    }
                } while (error);                

                if(cancel == false) {
                    System.out.print("Do you wish to keep the sorted file? Y/N (type A to keep all the tmp files) ");
                    line = sc.nextLine().toLowerCase().trim();

                    int keep = 0; // 0 maintains last file
                    if (line.charAt(0) == 'a') keep = 1; // 1 keeps all files
                    else if (line.charAt(0) == 'n') keep = -1; // -1 destroys all files 
                
                    System.out.print("Type the number of paths: ");
                    paths = Integer.parseInt(sc.nextLine());
                    System.out.print("Type the limit of ram: ");
                    ram = Integer.parseInt(sc.nextLine());
                    System.out.println("Sorting database. This may take a while.");
                    System.out.println();
                    bms = new BalancedMergeSort(keep, ram, paths, key);
                    bms.sort();
                    System.out.println("-- Heroes sorted by " + key + " --");
                    System.out.println();
                }
            }

            // Stop program execution
            else if (line.toLowerCase().charAt(0) == 's')  {
                System.out.println();
                System.out.println("-- Quitting --");
                System.out.print("Would you like to drop the database before leaving? Y/N ");
                line = sc.nextLine().trim().toLowerCase();
                if (line.charAt(0) == 'n') {
                    System.out.println("-- Database was not dropped --");
                } else {
                    if(EraseFiles.eraseAllFilesInDirectory("output/")) System.out.println("-- Database dropped successfully --");
                    else System.out.println("-- Error --");
                }
                break; // Exit the loop and stop the program
            }
        }
        raf.close();
        sc.close();
    }
}