package fox.btree;

/**
 * Created by tim on 08/12/16.
 */
public interface Node {

    Node getParent();

    int getNodeNum();

    boolean isRoot();

    boolean isLeaf();

    Object find(Comparable key);

    Object remove(Comparable key);

    void insert(Comparable key, Object value);

    void dump();

    int numKeys();

    int numValues();

    int numChildren();

    Comparable getKey(int pos);

    Object getValue(int pos);

    Node getChild(int pos);
}
