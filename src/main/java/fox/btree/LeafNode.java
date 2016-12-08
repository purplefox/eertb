package fox.btree;

/**
 * Created by tim on 08/12/16.
 */
public class LeafNode extends BaseNode {

    private int numKeys;
    private Comparable[] keys;
    private Object[] values;

    public LeafNode(BasicBplusTree tree) {
        super(tree);
        int b = tree.branchingFactor();
        keys = new Comparable[b];
        values = new Object[b];
    }

    @Override
    public boolean isLeaf() {
        return true;
    }

    @Override
    public Object find(Comparable key) {
        // TODO - binary search!
        for (int i = 0; i < numKeys; i++) {
            Comparable k = keys[i];
            if (k.equals(key)) {
                return values[i];
            }
        }
        return null;
    }

    @Override
    public Object remove(Comparable key) {
        // TODO - binary search!
        for (int i = 0; i < numKeys; i++) {
            Comparable k = keys[i];
            if (k.equals(key)) {
                Object val = values[i];
                removeFromArray(keys, i, numKeys);
                removeFromArray(values, i, numKeys);
                numKeys--;
                tree.addKeyCount(-1);
                if (numKeys < tree.branchingFactor() / 2 - 1) {
                    // TODO
                    // TODO borrow steal etc!
                }
                return val;
            }
        }
        return null;
    }

    @Override
    public void insert(Comparable key, Object value) {
        // Insert sorted order
        int i = 0;
        for (; i < numKeys; i++) {
            int comp = key.compareTo(keys[i]);
            if (comp == 0) {
                // update the value
                values[i] = value;
                return;
            } else if (comp < 0) {
                break;
            }
        }

        insertInArray(keys, i, key);
        insertInArray(values, i, value);

        numKeys++;
        tree.addKeyCount(1);

        if (i == 0) {
            InternalNode par = parent;
            Node child = this;
            while (par != null && par.getChild(0) == child) {
                // Not strictly necessary but this updates the zeroth key in the parent that we don't use so it reflects
                // the left most value in the child node
                par.setKey(0, key);

                child = par;
                par = (InternalNode)par.getParent();
            }
        }

        if (numKeys == tree.branchingFactor()) {
            // No room - split
            split();
        }
    }

    @Override
    public LeafNode findLeaf(Comparable key) {
        return this;
    }

    @Override
    public int numKeys() {
        return numKeys;
    }

    @Override
    public int numValues() {
        return numKeys;
    }

    @Override
    public int numChildren() {
        return 0;
    }

    @Override
    public Comparable getKey(int pos) {
        return keys[pos];
    }

    @Override
    public Object getValue(int pos) {
        return values[pos];
    }

    @Override
    public Node getChild(int pos) {
        return null;
    }

    void split() {

        int b = tree.branchingFactor();

        Comparable[] keysLeft = new Comparable[b];
        Object[] valuesLeft = new Object[b];

        Comparable[] keysRight = new Comparable[b];
        Object[] valuesRight = new Object[b];

        System.arraycopy(keys, 0, keysLeft, 0, b / 2);
        System.arraycopy(values, 0, valuesLeft, 0, b / 2);

        System.arraycopy(keys, b / 2, keysRight, 0, b / 2);
        System.arraycopy(values, b / 2, valuesRight, 0, b / 2);


        keys = keysLeft;
        values = valuesLeft;
        numKeys = b / 2;

        LeafNode newNode = new LeafNode(tree);
        newNode.keys = keysRight;
        newNode.values = valuesRight;
        newNode.numKeys = b / 2;

        if (parent == null) {
            // Create a new root
            parent = new InternalNode(tree);

            newNode.setParent(parent);

            parent.addChildPointer(0, keys[0], this);
            parent.addChildPointer(1, newNode.keys[0], newNode);

            tree.setRoot(parent);
        } else {
            newNode.setParent(parent);
            parent.insertChild(this, newNode.keys[0], newNode);
        }

    }

}
