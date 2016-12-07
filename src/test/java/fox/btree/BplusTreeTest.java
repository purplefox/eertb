package fox.btree;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNotNull;

/**
 * Created by tim on 05/12/16.
 */
public class BplusTreeTest {

    protected final int B = 4;

    protected BplusTree tree = new BplusTree(B);

    @Test
    public void testEmptyTree() {
        assertEquals(1, tree.nodeCount());
        BplusTree.Node root = tree.getRoot();
        assertNotNull(root);
        assertEquals(0, root.numChildren());
        assertEquals(0, root.numKeys());
        assertEquals(0, root.numValues());
    }

    @Test
    public void testRandomInsertFind() {
        insertRandom(1000);
    }

    private void insertRandom(int num) {
        List<Integer> keys = new ArrayList<>();
        for (int i = 0; i < num; i++) {
            keys.add(i);
        }
        Collections.shuffle(keys);
        assertEquals(1, tree.nodeCount());
        for (Integer i: keys) {
            //System.out.println("====================inserting " + i);
            tree.insert(i, "val" + i);
            checkInvariants(tree);
        }
        tree.dump();
    }


    protected void checkInvariants(BplusTree tree) {
        leafDepth = -1;
        checkInvariants(tree.getRoot(), null, null, 0);
    }

    private int leafDepth;

    protected void checkInvariants(BplusTree.Node node, Comparable greaterOrEqualTo, Comparable lessThan, int depth) {

        // Make sure number of keys, values and children are consistent
        if (node.isLeaf()) {
            assertEquals(node.numKeys(), node.numValues());
            assertEquals(0, node.numChildren());

            // Invariant: all leaves must be at same depth
            if (leafDepth != -1) {
                assertEquals(leafDepth, depth);
            } else {
                leafDepth = depth;
            }
        } else {
            assertEquals(node.numKeys(), node.numChildren());
            assertEquals(0, node.numValues());
        }

        // TODO add extra check that parent relatiopnship is correct
        // i.e. does the parent contain the child

        // Invariants on number of keys
        if (node.isRoot()) {
            if (node.isLeaf()) {
                // Root is only node in tree
                assertTrue(node.numKeys() >= 1);
                assertTrue(node.numKeys() <= B - 1);
            } else {
                assertTrue(node.numKeys() >= 2);
                assertTrue(node.numKeys() <= B);
            }
        } else if (node.isLeaf()) {
            assertTrue(node.numKeys() >=  B / 2 - 1);
            assertTrue(node.numKeys() <= B - 1);
        } else {
            assertTrue(node.numKeys() >=  B / 2);
            assertTrue(node.numKeys() <= B);
        }


        Comparable prev = null;
        for (int i = 0; i < node.numKeys(); i++) {
            Comparable key = node.getKey(i);

            // Invariant: key range
            if (lessThan != null) {
                assertTrue("key:" + key + " lt:" + lessThan, key.compareTo(lessThan) < 0);
            }
            if (greaterOrEqualTo != null) {
                assertTrue("key:" + key + " gte:" + greaterOrEqualTo, key.compareTo(greaterOrEqualTo) >= 0);
            }

            // Check the values are correct for a leaf
            if (node.isLeaf()) {
                Object val = node.getValue(i);
                assertEquals("val" + key, val);
            }

            // Invariant: keys must be in sorted order
            if (prev != null) {
                assertTrue(key.compareTo(prev) >= 0);
            }
            prev = key;

        }

        // Recurse
        if (!node.isLeaf()) {
            for (int i = 1; i < node.numKeys(); i++) {
                BplusTree.Node leftChild = node.getChild(i - 1);
                checkInvariants(leftChild, i == 1 ?  null : node.getKey(i - 1), node.getKey(i), depth + 1);
            }
            BplusTree.Node rightChild = node.getChild(node.numKeys() - 1);
            checkInvariants(rightChild, node.getKey(node.numKeys() - 1), null, depth + 1);

        }

        // Check that parent-child relationship is consistent in both directions
        if (node.getParent() != null) {
            boolean found = false;
            for (int i = 0; i < node.getParent().numKeys(); i++) {
                BplusTree.Node child = node.getParent().getChild(i);
                if (child == node) {
                    found = true;
                    break;
                }
            }
            assertTrue(found);
        }

    }
}
