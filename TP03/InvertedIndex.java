// import java.io.*;
// import java.util.*;

// public class InvertedIndex {

//     private Map<String, List<Integer>> index; // term -> list of hero IDs
//     private String filePath;

//     public InvertedIndex(String filePath) {
//         this.filePath = filePath;
//         this.index = new HashMap<>();
//         loadIndexFromFile(); // Load the index from file on initialization
//     }

//     // Method to load the index from the CSV file
//     private void loadIndexFromFile() {
//         File file = new File(filePath);
//         if (!file.exists())  return; // If file doesn't exist, start with an empty index
//         try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
//             String line;
//             while ((line = br.readLine()) != null) {
//                 String[] parts = line.split(",");
//                 String term = parts[0];
//                 List<Integer> heroIds = new ArrayList<>();
//                 for (int i = 1; i < parts.length; i++) {
//                     heroIds.add(Integer.parseInt(parts[i]));
//                 }
//                 index.put(term, heroIds);
//             }
//         } catch (IOException e) {
//             e.printStackTrace();
//         }
//     }

//     // Method to search for a term in the index
//     public List<Integer> search(String term) {
//         return index.getOrDefault(term, new ArrayList<>()); // Return the list of IDs or empty list if not found
//     }

//     // Method to add a term and corresponding hero IDs to the index
//     public void addToIndex(String term, int heroId) {
//         index.computeIfAbsent(term, k -> new ArrayList<>()).add(heroId);
//     }

//     // Method to persist the index back to the file
//     public void saveIndexToFile() {
//         try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath))) {
//             for (Map.Entry<String, List<Integer>> entry : index.entrySet()) {
//                 String term = entry.getKey();
//                 List<Integer> heroIds = entry.getValue();
//                 bw.write(term);
//                 for (int id : heroIds) {
//                     bw.write("," + id);
//                 }
//                 bw.newLine();
//             }
//         } catch (IOException e) {
//             e.printStackTrace();
//         }
//     }
// }
