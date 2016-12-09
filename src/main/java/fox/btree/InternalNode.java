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
    protected int minKeys() {
        return tree.branchingFactor() / 2;
    }

    @Override
    protected int maxKeys() {
        return tree.branchingFactor();
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
        for (Node c : children) {
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
        if (numKeys == maxKeys() + 1) {
            // No room - split
            split();
        }
    }

    private void split() {

        int splitAt = numKeys / 2 + 1;

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

    void removeKey(int pos) {
        removeFromArray(keys, pos, numKeys);
        removeFromArray(children, pos, numKeys);
        numKeys--;
        if (!isRoot()) {
            if (numKeys < tree.branchingFactor() / 2) {
                if (!tryStealSibling()) {
                    mergeSibling();
                }
            }
        } else {
            if (numKeys == 1) {
                // Child becomes root
                BaseNode child = children[0];
                child.setParent(null);
                tree.setRoot(child);
            }
        }
    }

    private boolean tryStealSibling() {
        int numSiblings = parent.numChildren();
        for (int i = 0; i < numSiblings; i++) {
            InternalNode sibling = (InternalNode)parent.getChild(i);
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

    private boolean tryStealSibling(InternalNode sibling, int siblingPos, boolean left) {

        int siblingNumKeys = sibling.numKeys();
        if (siblingNumKeys > minKeys()) {
            // It has spare key(s)

            int posToSteal = left ? sibling.numKeys - 1 : 0;
            int posToInsert = left ? 0 : numKeys;

            Comparable key = sibling.getKey(posToSteal);
            Node child = sibling.getChild(posToSteal);
            removeFromArray(sibling.keys, posToSteal, siblingNumKeys);
            removeFromArray(sibling.children, posToSteal, siblingNumKeys);
            sibling.numKeys--;
            insertInArray(keys, posToInsert, key);
            insertInArray(children, posToInsert, child);
            numKeys++;
            // Update parent key value
            if (left) {
                parent.setKey(siblingPos + 1, key);
            } else {
                parent.setKey(siblingPos, sibling.keys[0]);
            }
            // Update parent of child
            children[posToInsert].parent = this;
            return true;
        }
        return false;
    }

    private void mergeSibling() {
        int numSiblings = parent.numChildren();
        for (int i = 0; i < numSiblings; i++) {
            InternalNode sibling = (InternalNode)parent.getChild(i);
            boolean left;
            if (i < numSiblings - 1 && parent.getChild(i + 1) == this) {
                left = true;
            } else if (i > 0 && parent.getChild(i - 1) == this) {
                left = false;
            } else {
                continue;
            }
            // left or right sibling
            if (sibling.numKeys == minKeys()) {
                // Has min number of keys so can merge it
                mergeSibling(sibling, left, i);
                return;
            }
        }
    }

    private void mergeSibling(InternalNode sibling, boolean left, int siblingPos) {
        Comparable[] destKeys;
        Node[] destChildren;

        Comparable[] srcKeys;
        Node[] srcChildren;

        InternalNode dest;
        InternalNode src;

        int srcPos;

        if (left) {
            destKeys = sibling.keys;
            destChildren = sibling.children;
            srcKeys = keys;
            srcChildren = children;
            dest = sibling;
            src = this;
            srcPos = siblingPos + 1;
        } else {
            destKeys = keys;
            destChildren = children;
            srcKeys = sibling.keys;
            srcChildren = sibling.children;
            dest = this;
            src = sibling;
            srcPos = siblingPos;
        }

        // Update parents of children
        for (int i = 0; i < src.numKeys; i++) {
            src.children[i].setParent(dest);
        }

        System.arraycopy(srcKeys, 0, destKeys, dest.numKeys, src.numKeys);
        System.arraycopy(srcChildren, 0, destChildren, dest.numKeys, src.numKeys);

        dest.numKeys += src.numKeys;

        // remove merged key from parent
        parent.removeKey(srcPos);


    }
}
