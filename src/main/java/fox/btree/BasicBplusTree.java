package fox.btree;

/**
 * A very basic B+Tree used to experiment and play with the structure.
 *
 * Not designed for real-world usage
 *
 * Created by tim on 29/11/16.
 */
public class BasicBplusTree {

    private final int branchingFactor;
    private int keycount;
    private int nodeCount;
    private Node root;

    public BasicBplusTree(int branchingFactor) {
        this.branchingFactor = branchingFactor;
        this.root = new LeafNode(this);
    }

    public void insert(Comparable key, Object value) {
        root.insert(key, value);
        keycount++;
    }

    public Object find(Comparable key) {
       return root.find(key);
    }

    public Object remove(Comparable key) {
        return root.remove(key);
    }

    public Node getRoot() {
        return root;
    }

    public int nodeCount() {
        return nodeCount;
    }

    public int keyCount() {
        return keycount;
    }

    public void dump() {
        root.dump();
    }

    int nextNodeCount() {
        return nodeCount++;
    }

    int branchingFactor() {
        return branchingFactor;
    }

    void setRoot(Node root) {
        this.root = root;
    }


}
