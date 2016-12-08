package fox.btree;

import java.util.*;

/**
 *
 *
 *
 * Created by tim on 29/11/16.
 */
public class BasicBplusTree {

    private final int b;

    private int keycount;
    private int nodeCount;

    private Node root;

    public BasicBplusTree(int b) {
        this.b = b;
        this.root = new LeafNode();
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

    public void dump() {
        dump(root);
    }

    Node getRoot() {
        return root;
    }

    int nodeCount() {
        return nodeCount;
    }

    int keyCount() {
        return keycount;
    }

    public void dump(Node root){
        Queue<Node> level  = new LinkedList<>();
        level.add(root);
        while(!level.isEmpty()){
            int count = level.size();
            for (int i = 0; i < count; i++) {
                Node node = level.poll();
                System.out.print("N" + node.getNodeNum());
                if (node.getParent() != null) {
                    System.out.print("|P" + node.getParent().getNodeNum() + "|");
                } else {
                    System.out.print("|root|");
                }
                for (int j = 0; j < node.numKeys(); j++) {
                    Comparable key = node.getKey(j);
                    System.out.print(key + "|");
                }
                System.out.print("  ");

                if (!node.isLeaf()) {
                    for (int j = 0; j < node.numKeys(); j++) {
                        Node child = node.getChild(j);
                        level.add(child);
                    }
                }
            }
            System.out.print("\n");
        }
    }

    public interface Node {
        
        Node getParent();

        int getNodeNum();
        
        boolean isRoot();
        
        boolean isLeaf();
        
        void setParent(InternalNode parent);

        Object find(Comparable key);

        Object remove(Comparable key);
        
        void insert(Comparable key, Object value);

        LeafNode findLeaf(Comparable key);

        int numKeys();

        int numValues();

        int numChildren();

        Comparable getKey(int pos);

        Object getValue(int pos);

        Node getChild(int pos);

    }
    
    abstract class BaseNode implements Node {

        protected int nodeNum = nodeCount++;

        protected InternalNode parent;

        @Override
        public Node getParent() {
            return parent;
        }

        @Override
        public int getNodeNum() {
            return nodeNum;
        }

        @Override
        public boolean isRoot() {
            return parent == null;
        }

        @Override
        public void setParent(InternalNode parent) {
            this.parent = parent;
        }
    }
    
    class LeafNode extends BaseNode {

        private int numKeys;
        private Comparable[] keys = new Comparable[b];
        private Object[] values = new Object[b];


        @Override
        public boolean isRoot() {
            return parent == null;
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
                    if (numKeys < b / 2 - 1) {
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

            if (numKeys == b) {
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

            LeafNode newNode = new LeafNode();
            newNode.keys = keysRight;
            newNode.values = valuesRight;
            newNode.numKeys = b / 2;

            if (parent == null) {
                // Create a new root
                parent = new InternalNode();

                newNode.setParent(parent);

                parent.addChildPointer(0, keys[0], this);
                parent.addChildPointer(1, newNode.keys[0], newNode);

                root = parent;
            } else {
                newNode.setParent(parent);
                parent.insertChild(this, newNode.keys[0], newNode);
            }

        }

    }
    
    class InternalNode extends BaseNode {

        private int numKeys;
        // A little extra space for splitting
        private Comparable[] keys = new Comparable[b + 1];
        private Node[] children = new Node[b + 1];

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
            if (numKeys == b + 1) {
                // No room - split
                split();
            }
        }

        @Override
        public boolean isRoot() {
            return parent == null;
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
                    Node child = children[i - 1];
                    return child.findLeaf(key);
                }
            }
            Node child = children[numKeys - 1];
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

            Comparable[] keysLeft = new Comparable[b + 1];
            Node[] childrenLeft = new Node[b + 1];

            Comparable[] keysRight = new Comparable[b + 1];
            Node[] childrenRight = new Node[b + 1];

            System.arraycopy(keys, 0, keysLeft, 0, splitAt);
            System.arraycopy(children, 0, childrenLeft, 0, splitAt);

            System.arraycopy(keys, splitAt, keysRight, 0, numKeys - splitAt);
            System.arraycopy(children, splitAt, childrenRight, 0, numKeys - splitAt);

            int leftKeys = splitAt;
            int rightKeys = numKeys - splitAt;

            keys = keysLeft;
            children = childrenLeft;
            numKeys = leftKeys;

            InternalNode newNode = new InternalNode();
            newNode.keys = keysRight;
            newNode.children = childrenRight;
            newNode.numKeys = rightKeys;

            for (int i = 0; i < newNode.numKeys; i++) {
                Node c = newNode.getChild(i);
                c.setParent(newNode);
            }

            if (parent == null) {
                // Create a new root
                parent = new InternalNode();
                parent.addChildPointer(0, keys[0], this);
                parent.addChildPointer(1, newNode.keys[0], newNode);
                newNode.setParent(parent);
                root = parent;
            } else {
                newNode.setParent(parent);
                parent.insertChild(this, newNode.keys[0], newNode);
            }
        }
    }

    private <T> void insertInArray(T[] arr, int pos, T val) {
        System.arraycopy(arr, pos, arr, pos + 1, arr.length - pos - 1);
        arr[pos] = val;
    }

    private <T> void removeFromArray(T[] arr, int pos, int numKeys) {
        System.arraycopy(arr, pos + 1, arr, pos, numKeys - pos - 1);
        arr[numKeys - 1] = null;
    }

}
