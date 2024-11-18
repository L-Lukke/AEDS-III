import java.io.*;
import java.util.*;

public class BTree {
    private static final int ORDER = 8;
    private BTreeNode root;
    private final String indexFilePath;

    public BTree(String indexFilePath) {
        this.indexFilePath = indexFilePath;
        this.root = new BTreeNode();
        readBTreeFromFile();
    }

    private static class BTreeNode {
        List<Integer> keys;
        List<Long> addresses; // Addresses for the corresponding keys
        List<BTreeNode> children; // Children nodes
        boolean isLeaf;

        BTreeNode() {
            this.keys = new ArrayList<>();
            this.addresses = new ArrayList<>();
            this.children = new ArrayList<>();
            this.isLeaf = true;
        }
    }

    public void insert(int key, long address) {
        BTreeNode r = root;
        if (r.keys.size() == ORDER - 1) { // If root is full, split
            BTreeNode s = new BTreeNode();
            root = s;
            s.isLeaf = false;
            s.children.add(r);
            splitChild(s, 0);
            insertNonFull(s, key, address);
        } else {
            insertNonFull(r, key, address);
        }
        writeBTreeToFile();
    }

    private void insertNonFull(BTreeNode node, int key, long address) {
        int i = node.keys.size() - 1;

        if (node.isLeaf) {
            // Insert the new key at the correct position in the leaf node
            while (i >= 0 && key < node.keys.get(i)) {
                i--;
            }
            node.keys.add(i + 1, key);
            node.addresses.add(i + 1, address);
        } else {
            // Find the child where the new key should go
            while (i >= 0 && key < node.keys.get(i)) {
                i--;
            }
            i++;
            BTreeNode child = node.children.get(i);
            if (child.keys.size() == ORDER - 1) {
                splitChild(node, i);
                if (key > node.keys.get(i)) {
                    i++;
                }
            }
            insertNonFull(node.children.get(i), key, address);
        }
    }

    private void splitChild(BTreeNode parent, int i) {
        BTreeNode fullChild = parent.children.get(i);
        BTreeNode newChild = new BTreeNode();
        newChild.isLeaf = fullChild.isLeaf;

        // Move the median key up to the parent
        parent.keys.add(i, fullChild.keys.get(ORDER / 2 - 1));
        parent.addresses.add(i, fullChild.addresses.get(ORDER / 2 - 1));
        parent.children.add(i + 1, newChild);

        // Split the full child's keys and children
        newChild.keys.addAll(fullChild.keys.subList(ORDER / 2, fullChild.keys.size()));
        newChild.addresses.addAll(fullChild.addresses.subList(ORDER / 2, fullChild.addresses.size()));
        fullChild.keys.subList(ORDER / 2 - 1, fullChild.keys.size()).clear();
        fullChild.addresses.subList(ORDER / 2 - 1, fullChild.addresses.size()).clear();

        if (!fullChild.isLeaf) {
            newChild.children.addAll(fullChild.children.subList(ORDER / 2, fullChild.children.size()));
            fullChild.children.subList(ORDER / 2, fullChild.children.size()).clear();
        }
    }

    public long search(int key) {
        return search(root, key, 0);
    }

    private long search(BTreeNode node, int key, int level) {
        int i = 0;

        while (i < node.keys.size() && key > node.keys.get(i)) i++;

        System.out.println("Searching in Node: " + node.keys + " of level " + level++);

        if (i < node.keys.size() && key == node.keys.get(i)) {
            return node.addresses.get(i);
        } else if (node.isLeaf) {
            return -1;
        } else {
            return search(node.children.get(i), key, level);
        }
    }
    
    public void remove(int key) {
        remove(root, key);
    
        // If the root node becomes empty, make its first child the new root (if it has any children)
        if (root.keys.isEmpty() && !root.isLeaf) {
            root = root.children.get(0);
        }
        writeBTreeToFile();
    }
    
    private void remove(BTreeNode node, int key) {
        int idx = findKey(node, key);
    
        // Case 1: Key is found in this node
        if (idx < node.keys.size() && node.keys.get(idx) == key) {
            if (node.isLeaf) {
                // Case 1a: Key is in a leaf node
                node.keys.remove(idx);
                node.addresses.remove(idx);
            } else {
                // Case 1b: Key is in an internal node
                removeInternalNodeKey(node, key, idx);
            }
        } else {
            // Case 2: Key is not in this node
            if (node.isLeaf) {
                return; // Key is not in the tree
            }
    
            boolean lastChild = (idx == node.keys.size());
    
            // If the child node where the key is supposed to be has fewer than the minimum required keys, fix it
            if (node.children.get(idx).keys.size() < ORDER / 2) {
                fixChild(node, idx);
            }
    
            // After fixing, proceed to remove the key from the correct child
            if (lastChild && idx > node.keys.size()) {
                remove(node.children.get(idx - 1), key);
            } else {
                remove(node.children.get(idx), key);
            }
        }
    }
    
    private void removeInternalNodeKey(BTreeNode node, int key, int idx) {
        BTreeNode leftChild = node.children.get(idx);
        BTreeNode rightChild = node.children.get(idx + 1);
    
        // Case 1b-1: Left child has enough keys
        if (leftChild.keys.size() >= ORDER / 2) {
            int predecessorKey = getPredecessor(leftChild);
            long predecessorAddress = leftChild.addresses.get(leftChild.keys.indexOf(predecessorKey));
            node.keys.set(idx, predecessorKey);
            node.addresses.set(idx, predecessorAddress);
            remove(leftChild, predecessorKey);
        }
        // Case 1b-2: Right child has enough keys
        else if (rightChild.keys.size() >= ORDER / 2) {
            int successorKey = getSuccessor(rightChild);
            long successorAddress = rightChild.addresses.get(rightChild.keys.indexOf(successorKey));
            node.keys.set(idx, successorKey);
            node.addresses.set(idx, successorAddress);
            remove(rightChild, successorKey);
        }
        // Case 1b-3: Neither child has enough keys, merge the children
        else {
            merge(node, idx);
            remove(leftChild, key);
        }
    }
    
    private int findKey(BTreeNode node, int key) {
        int idx = 0;
        while (idx < node.keys.size() && key > node.keys.get(idx)) idx++;
        return idx;
    }
    
    private int getPredecessor(BTreeNode node) {
        // The predecessor is the rightmost key in the left subtree
        while (!node.isLeaf) {
            node = node.children.get(node.keys.size());
        }
        return node.keys.get(node.keys.size() - 1);
    }
    
    private int getSuccessor(BTreeNode node) {
        // The successor is the leftmost key in the right subtree
        while (!node.isLeaf) {
            node = node.children.get(0);
        }
        return node.keys.get(0);
    }
    
    private void fixChild(BTreeNode node, int idx) {
        BTreeNode child = node.children.get(idx);
        BTreeNode leftSibling = (idx > 0) ? node.children.get(idx - 1) : null;
        BTreeNode rightSibling = (idx < node.children.size() - 1) ? node.children.get(idx + 1) : null;
    
        // Case 2a: Borrow from left sibling
        if (leftSibling != null && leftSibling.keys.size() >= ORDER / 2) {
            child.keys.add(0, node.keys.get(idx - 1));
            child.addresses.add(0, node.addresses.get(idx - 1));
            if (!child.isLeaf) {
                child.children.add(0, leftSibling.children.remove(leftSibling.children.size() - 1));
            }
            node.keys.set(idx - 1, leftSibling.keys.remove(leftSibling.keys.size() - 1));
            node.addresses.set(idx - 1, leftSibling.addresses.remove(leftSibling.addresses.size() - 1));
        }
        // Case 2b: Borrow from right sibling
        else if (rightSibling != null && rightSibling.keys.size() >= ORDER / 2) {
            child.keys.add(node.keys.get(idx));
            child.addresses.add(node.addresses.get(idx));
            if (!child.isLeaf) {
                child.children.add(rightSibling.children.remove(0));
            }
            node.keys.set(idx, rightSibling.keys.remove(0));
            node.addresses.set(idx, rightSibling.addresses.remove(0));
        }
        // Case 2c: Merge with a sibling
        else {
            if (leftSibling != null) {
                merge(node, idx - 1);
            } else {
                merge(node, idx);
            }
        }
    }
    
    private void merge(BTreeNode node, int idx) {
        BTreeNode leftChild = node.children.get(idx);
        BTreeNode rightChild = node.children.get(idx + 1);
    
        // Pull the key down from the parent
        leftChild.keys.add(node.keys.remove(idx));
        leftChild.addresses.add(node.addresses.remove(idx));
    
        // Move all keys and children from the right child to the left child
        leftChild.keys.addAll(rightChild.keys);
        leftChild.addresses.addAll(rightChild.addresses);
        if (!rightChild.isLeaf) {
            leftChild.children.addAll(rightChild.children);
        }
    
        // Remove the right child from the parent
        node.children.remove(idx + 1);
    }    

    // Writing the B-Tree index to a file (similar to Hash class)
    private synchronized void writeBTreeToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(indexFilePath))) {
            writeNodeToFile(writer, root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeNodeToFile(BufferedWriter writer, BTreeNode node) throws IOException {
        writer.write(node.keys.size() + "\n");
        for (int i = 0; i < node.keys.size(); i++) {
            writer.write(node.keys.get(i) + " " + node.addresses.get(i) + "\n");
        }

        if (!node.isLeaf) {
            for (BTreeNode child : node.children) {
                writeNodeToFile(writer, child);
            }
        }
    }

    // Reading the B-Tree index from a file (similar to Hash class)
    private void readBTreeFromFile() {
        File file = new File(indexFilePath);
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            root = readNodeFromFile(reader);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private BTreeNode readNodeFromFile(BufferedReader reader) throws IOException {
        BTreeNode node = new BTreeNode();
        int keyCount = Integer.parseInt(reader.readLine());
        for (int i = 0; i < keyCount; i++) {
            String[] parts = reader.readLine().split(" ");
            int key = Integer.parseInt(parts[0]);
            long address = Long.parseLong(parts[1]);
            node.keys.add(key);
            node.addresses.add(address);
        }

        if (reader.ready()) {
            node.isLeaf = false;
            for (int i = 0; i <= keyCount; i++) {
                node.children.add(readNodeFromFile(reader));
            }
        }

        return node;
    }

    public void printState() {
        printNode(root, 0);
    }

    private void printNode(BTreeNode node, int level) {
        System.out.println("Level " + level + " " + node.keys);
        if (!node.isLeaf) {
            for (BTreeNode child : node.children) printNode(child, level + 1);
        }
    }

    // public void printStateRecursive() {
    //     printStateRecursive(root, 0);
    // }
    
    // private void printStateRecursive(BTreeNode node, int level) {
    //     System.out.println("Level " + level + " Keys: " + node.keys);
    
    //     if (node.isLeaf) {
    //         System.out.println("Leaf Node at Level " + level + " contains keys: " + node.keys + " and addresses: " + node.addresses);
    //     } else {
    //         for (BTreeNode child : node.children) {
    //             printStateRecursive(child, level + 1);
    //         }
    //     }
    // }
}
