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
                if (!isRoot() && numKeys < tree.branchingFactor() / 2 - 1) {
                    if (!tryStealSibling()) {
                        mergeSibling();
                    }
                }
                return val;
            }
        }
        return null;
    }

    private boolean tryStealSibling() {
        int numSiblings = parent.numChildren();
        for (int i = 0; i < numSiblings; i++) {
            LeafNode sibling = (LeafNode)parent.getChild(i);
            if (i < numSiblings - 1 && parent.getChild(i + 1) == this) {
                // left sibling
                if (tryStealSibling(sibling, i, true)) {
                    return true;
                }
            }
            if (i > 0 && parent.getChild(i - 1) == this) {
                // Right sibling
                if (tryStealSibling(sibling, i, false)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean tryStealSibling(LeafNode sibling, int siblingPos, boolean left) {
        int siblingNumKeys = sibling.numKeys();
        if (siblingNumKeys >= tree.branchingFactor() / 2) {
            // It has spare key(s)

            int posToSteal = left ? sibling.numKeys - 1 : 0;
            int posToInsert = left ? 0 : numKeys;

            Comparable key = sibling.getKey(posToSteal);
            Object value = sibling.getValue(posToSteal);
            removeFromArray(sibling.keys, posToSteal, siblingNumKeys);
            removeFromArray(sibling.values, posToSteal, siblingNumKeys);
            sibling.numKeys--;
            insertInArray(keys, posToInsert, key);
            insertInArray(values, posToInsert, value);
            numKeys++;
            // Update parent key value
            if (left) {
                parent.setKey(siblingPos + 1, getKey(0));
            } else {
                parent.setKey(siblingPos, sibling.getKey(0));
            }
            return true;
        }
        return false;
    }

    private void mergeSibling() {
        int numSiblings = parent.numChildren();
        for (int i = 0; i < numSiblings; i++) {
            LeafNode sibling = (LeafNode)parent.getChild(i);
            boolean left;
            if (i < numSiblings - 1 && parent.getChild(i + 1) == this) {
                left = true;
            } else if (i > 0 && parent.getChild(i - 1) == this) {
                left = false;
            } else {
                continue;
            }
            // left or right sibling
            if (sibling.numKeys == tree.branchingFactor() / 2 - 1) {
                // Has min number of keys so can merge it
                mergeSibling(sibling, left, i);
                return;
            }
        }
    }

    private void mergeSibling(LeafNode sibling, boolean left, int siblingPos) {
        Comparable[] destKeys;
        Object[] destValues;

        Comparable[] srcKeys;
        Object[] srcValues;

        LeafNode dest;
        LeafNode src;

        int srcPos;

        if (left) {
            destKeys = sibling.keys;
            destValues = sibling.values;
            srcKeys = keys;
            srcValues = values;
            dest = sibling;
            src = this;
            srcPos = siblingPos + 1;
        } else {
            destKeys = keys;
            destValues = values;
            srcKeys = sibling.keys;
            srcValues = sibling.values;
            dest = this;
            src = sibling;
            srcPos = siblingPos;
        }

        System.arraycopy(srcKeys, 0, destKeys, dest.numKeys, src.numKeys);
        System.arraycopy(srcValues, 0, destValues, dest.numKeys, src.numKeys);

        dest.numKeys += src.numKeys;

        // remove merged key from parent
        parent.removeKey(srcPos);
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

//        if (i == 0) {
//            InternalNode par = parent;
//            Node child = this;
//            while (par != null && par.getChild(0) == child) {
//                // Not strictly necessary but this updates the zeroth key in the parent that we don't use so it reflects
//                // the left most value in the child node
//                par.setKey(0, key);
//
//                child = par;
//                par = (InternalNode)par.getParent();
//            }
//        }

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
