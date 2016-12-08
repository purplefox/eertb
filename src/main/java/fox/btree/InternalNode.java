package fox.btree;

/**
 * Created by tim on 08/12/16.
 */
public class InternalNode extends BaseNode {

    private int numKeys;
    private Comparable[] keys;
    private BaseNode[] children;

    public InternalNode(BasicBplusTree tree) {
        super(tree);
        int b = tree.branchingFactor();
        // A little extra space for splitting
        keys = new Comparable[b + 1];
        children = new BaseNode[b + 1];
    }

    void addChildPointer(int pos, Comparable key, Node child) {
        insertInArray(keys, pos, key);
        insertInArray(children, pos, child);
        numKeys++;
    }

    void setKey(int pos, Comparable key) {
        keys[pos] = key;
    }

    void insertChild(Node position, Comparable key, Node child) {

        int pos = 0;
        boolean found = false;
        for (Node c: children) {
            if (c == position) {
                found = true;
                break;
            }
            pos++;
        }

        if (!found) {
            throw new IllegalStateException("Can't find node");
        }

        pos++;

        // insert key at position i
        insertInArray(keys, pos, key);
        insertInArray(children, pos, child);

        numKeys++;

        // insert child at position i + 1

        // For inner nodes the first key is effectively ignored so we split at b + 1
        // (see visualisation)
        if (numKeys == tree.branchingFactor() + 1) {
            // No room - split
            split();
        }
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public Object find(Comparable key) {
        Node leaf = findLeaf(key);
        if (leaf != null) {
            return leaf.find(key);
        } else {
            return null;
        }
    }

    @Override
    public Object remove(Comparable key) {
        Node leaf = findLeaf(key);
        if (leaf != null) {
            return leaf.remove(key);
        } else {
            return null;
        }
    }

    @Override
    public void insert(Comparable key, Object value) {
        Node leaf = findLeaf(key);
        leaf.insert(key, value);
    }

    @Override
    public LeafNode findLeaf(Comparable key) {
        for (int i = 1; i < numKeys; i++) {
            Comparable k = keys[i];
            int comp = key.compareTo(k);
            if (comp < 0) {
                BaseNode child = children[i - 1];
                return child.findLeaf(key);
            }
        }
        BaseNode child = children[numKeys - 1];
        return child.findLeaf(key);
    }

    @Override
    public int numKeys() {
        return numKeys;
    }

    @Override
    public int numValues() {
        return 0;
    }

    @Override
    public int numChildren() {
        return numKeys;
    }

    @Override
    public Comparable getKey(int pos) {
        return keys[pos];
    }

    @Override
    public Object getValue(int pos) {
        return null;
    }

    @Override
    public Node getChild(int pos) {
        return children[pos];
    }

    void split() {

        int splitAt = numKeys / 2;

        int b = tree.branchingFactor();

        Comparable[] keysLeft = new Comparable[b + 1];
        BaseNode[] childrenLeft = new BaseNode[b + 1];

        Comparable[] keysRight = new Comparable[b + 1];
        BaseNode[] childrenRight = new BaseNode[b + 1];

        System.arraycopy(keys, 0, keysLeft, 0, splitAt);
        System.arraycopy(children, 0, childrenLeft, 0, splitAt);

        System.arraycopy(keys, splitAt, keysRight, 0, numKeys - splitAt);
        System.arraycopy(children, splitAt, childrenRight, 0, numKeys - splitAt);

        int leftKeys = splitAt;
        int rightKeys = numKeys - splitAt;

        keys = keysLeft;
        children = childrenLeft;
        numKeys = leftKeys;

        InternalNode newNode = new InternalNode(tree);
        newNode.keys = keysRight;
        newNode.children = childrenRight;
        newNode.numKeys = rightKeys;

        for (int i = 0; i < newNode.numKeys; i++) {
            BaseNode c = newNode.children[i];
            c.setParent(newNode);
        }

        if (parent == null) {
            // Create a new root
            parent = new InternalNode(tree);
            parent.addChildPointer(0, keys[0], this);
            parent.addChildPointer(1, newNode.keys[0], newNode);
            newNode.setParent(parent);
            tree.setRoot(parent);
        } else {
            newNode.setParent(parent);
            parent.insertChild(this, newNode.keys[0], newNode);
        }
    }
}
