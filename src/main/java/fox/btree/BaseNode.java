package fox.btree;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by tim on 08/12/16.
 */
public abstract class BaseNode implements Node {

    protected final BasicBplusTree tree;
    protected final int nodeNum;

    protected InternalNode parent;

    public BaseNode(BasicBplusTree tree) {
        this.tree = tree;
        this.nodeNum = tree.nextNodeCount();
    }

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

    public void setParent(InternalNode parent) {
        this.parent = parent;
    }

    public abstract LeafNode findLeaf(Comparable key);

    public void dump() {
        Queue<Node> level = new LinkedList<>();
        level.add(this);
        while (!level.isEmpty()) {
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

    protected <T> void insertInArray(T[] arr, int pos, T val) {
        System.arraycopy(arr, pos, arr, pos + 1, arr.length - pos - 1);
        arr[pos] = val;
    }

    protected <T> void removeFromArray(T[] arr, int pos, int numKeys) {
        System.arraycopy(arr, pos + 1, arr, pos, numKeys - pos - 1);
        arr[numKeys - 1] = null;
    }

}
