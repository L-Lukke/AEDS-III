// WIP. NOT WORKING ATM.

// import java.io.*;
// import java.util.*;

// public class BTreeStar {
//     private static final int ORDER = 8;
//     private Node root;
//     private final String indexFilePath;

//     public BTreeStar(String indexFilePath) {
//         this.indexFilePath = indexFilePath;
//         this.root = new LeafNode();  // Initially, root is a leaf
//         readIndexFromFile();
//     }

//     // Abstract base class for Node
//     abstract class Node {
//         List<Integer> keys;

//         Node() {
//             this.keys = new ArrayList<>();
//         }

//         abstract boolean isLeaf();
//     }

//     // Internal Node class
//     class InternalNode extends Node {
//         List<Node> children;

//         InternalNode() {
//             super();
//             this.children = new ArrayList<>();
//         }

//         @Override
//         boolean isLeaf() {
//             return false;
//         }
//     }

//     // Leaf Node class
//     class LeafNode extends Node {
//         List<Long> addresses;  // Address list corresponds to keys

//         LeafNode() {
//             super();
//             this.addresses = new ArrayList<>();
//         }

//         @Override
//         boolean isLeaf() {
//             return true;
//         }
//     }

//     // Add a new key-address pair to the B-Tree
//     public void addToIndex(int key, long address) {
//         Node newChild = insert(root, key, address);
//         if (newChild != null) {
//             // If root is split, create a new root
//             InternalNode newRoot = new InternalNode();
//             newRoot.keys.add(root.keys.get(root.keys.size() - 1));  // Largest key of old root
//             newRoot.children.add(root);
//             newRoot.children.add(newChild);
//             root = newRoot;
//         }
//         writeIndexToFile();
//     }

//     // Insert function with recursive splitting logic
//     private Node insert(Node node, int key, long address) {
//         if (node.isLeaf()) {
//             LeafNode leaf = (LeafNode) node;
//             int pos = Collections.binarySearch(leaf.keys, key);
//             if (pos >= 0) {
//                 leaf.addresses.set(pos, address);  // Update if key already exists
//             } else {
//                 pos = -pos - 1;
//                 leaf.keys.add(pos, key);
//                 leaf.addresses.add(pos, address);
//                 if (leaf.keys.size() > ORDER - 1) {
//                     return splitLeaf(leaf);  // Split if overflow occurs
//                 }
//             }
//         } else {
//             InternalNode internal = (InternalNode) node;
//             int pos = Collections.binarySearch(internal.keys, key);
//             if (pos >= 0) pos++;
//             else pos = -pos - 1;

//             Node newChild = insert(internal.children.get(pos), key, address);
//             if (newChild != null) {
//                 internal.keys.add(pos, internal.children.get(pos).keys.get(internal.children.get(pos).keys.size() - 1));
//                 internal.children.add(pos + 1, newChild);
//                 if (internal.keys.size() > ORDER - 1) {
//                     return splitInternal(internal);
//                 }
//             }
//         }
//         return null;  // No split occurred
//     }

//     // Split a leaf node
//     private Node splitLeaf(LeafNode leaf) {
//         LeafNode newLeaf = new LeafNode();
//         int mid = leaf.keys.size() / 2;

//         // Move half the entries to the new leaf
//         newLeaf.keys.addAll(leaf.keys.subList(mid, leaf.keys.size()));
//         newLeaf.addresses.addAll(leaf.addresses.subList(mid, leaf.addresses.size()));
//         leaf.keys.subList(mid, leaf.keys.size()).clear();
//         leaf.addresses.subList(mid, leaf.addresses.size()).clear();

//         return newLeaf;  // Return new node after split
//     }

//     // Split an internal node
//     private Node splitInternal(InternalNode internal) {
//         InternalNode newInternal = new InternalNode();
//         int mid = internal.keys.size() / 2;

//         // Move half the keys and children to the new internal node
//         newInternal.keys.addAll(internal.keys.subList(mid + 1, internal.keys.size()));
//         newInternal.children.addAll(internal.children.subList(mid + 1, internal.children.size()));
//         internal.keys.subList(mid, internal.keys.size()).clear();
//         internal.children.subList(mid + 1, internal.children.size()).clear();

//         return newInternal;  // Return new node after split
//     }

//     // Search for a key in the B-Tree
//     public long searchInIndex(int key) {
//         Node node = root;
//         while (!node.isLeaf()) {
//             InternalNode internal = (InternalNode) node;
//             int pos = Collections.binarySearch(internal.keys, key);
//             if (pos >= 0) pos++;
//             else pos = -pos - 1;
//             node = internal.children.get(pos);
//         }

//         LeafNode leaf = (LeafNode) node;
//         int pos = Collections.binarySearch(leaf.keys, key);
//         if (pos >= 0) {
//             return leaf.addresses.get(pos);  // Return address if key is found
//         }
//         return -1;  // Key not found
//     }

//     // Remove a key from the B-Tree
//     public void removeFromIndex(int key) {
//         Node node = root;
//         delete(node, key);
//         writeIndexToFile();
//     }

//     private boolean delete(Node node, int key) {
//         if (node.isLeaf()) {
//             LeafNode leaf = (LeafNode) node;
//             int pos = Collections.binarySearch(leaf.keys, key);
//             if (pos >= 0) {
//                 leaf.keys.remove(pos);
//                 leaf.addresses.remove(pos);
//                 return true;
//             }
//         } else {
//             InternalNode internal = (InternalNode) node;
//             int pos = Collections.binarySearch(internal.keys, key);
//             if (pos >= 0) pos++;
//             else pos = -pos - 1;

//             boolean deleted = delete(internal.children.get(pos), key);
//             if (deleted && internal.children.get(pos).keys.size() == 0) {
//                 internal.children.remove(pos);
//                 if (internal.keys.size() > pos - 1) {
//                     internal.keys.remove(pos - 1);
//                 }
//             }
//             return deleted;
//         }
//         return false;
//     }

//     // Read the B-Tree index from file
//     private synchronized void readIndexFromFile() {
//         File file = new File(indexFilePath);
//         if (!file.exists()) return; // If no index file exists, nothing to read

//         try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
//             String line;
//             while ((line = reader.readLine()) != null) {
//                 String[] parts = line.split(",");
//                 int key = Integer.parseInt(parts[0]);
//                 long address = Long.parseLong(parts[1]);
//                 addToIndex(key, address);  // Rebuild the tree from file
//             }
//         } catch (IOException e) {
//             e.printStackTrace();
//         }
//     }

//     // Write the B-Tree index to file
//     private synchronized void writeIndexToFile() {
//         try (BufferedWriter writer = new BufferedWriter(new FileWriter(indexFilePath))) {
//             writeNodeToFile(writer, root);
//         } catch (IOException e) {
//             e.printStackTrace();
//         }
//     }

//     // Helper to write a node (recursively) to a file
//     private void writeNodeToFile(BufferedWriter writer, Node node) throws IOException {
//         if (node.isLeaf()) {
//             LeafNode leaf = (LeafNode) node;
//             for (int i = 0; i < leaf.keys.size(); i++) {
//                 writer.write(leaf.keys.get(i) + "," + leaf.addresses.get(i) + "\n");
//             }
//         } else {
//             InternalNode internal = (InternalNode) node;
//             for (Node child : internal.children) {
//                 writeNodeToFile(writer, child);
//             }
//         }
//     }

//     // Print the state of the B-Tree for debugging purposes
//     public void printState() {
//         printNode(root, 0);
//     }

//     private void printNode(Node node, int level) {
//         System.out.println("Level " + level + ": " + node.keys);
//         if (!node.isLeaf()) {
//             InternalNode internal = (InternalNode) node;
//             for (Node child : internal.children) {
//                 printNode(child, level + 1);
//             }
//         }
//     }
// }
