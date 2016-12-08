package fox.btree;

import org.junit.Test;

import java.security.SecureRandom;
import java.util.*;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNotNull;

/**
 * Created by tim on 05/12/16.
 */
public class BasicBplusTreeTest {

    private Random random = new SecureRandom();

    protected final int B = 10;

    protected BasicBplusTree tree = new BasicBplusTree(B);

    @Test
    public void testEmptyTree() {
        assertEquals(1, tree.nodeCount());
        BasicBplusTree.Node root = tree.getRoot();
        assertNotNull(root);
        assertTrue(root.isRoot());
        assertTrue(root.isLeaf());
        assertEquals(0, root.numChildren());
        assertEquals(0, root.numKeys());
        assertEquals(0, root.numValues());
    }

    @Test
    public void testSimpleInsertFind() {
        tree.insert("key1", "val1");
        tree.insert("key2", "val2");
        tree.insert("key3", "val3");
        assertEquals("val1", tree.find("key1"));
        assertEquals("val2", tree.find("key2"));
        assertEquals("val3", tree.find("key3"));
    }

    @Test
    public void testNotFound() {
        assertNull(tree.find("key1"));
        tree.insert("key2", "val2");
        assertNull(tree.find("key1"));
        tree.insert("key1", "val1");
        assertEquals("val1", tree.find("key1"));
    }

    @Test
    public void testRemove() {
        tree.insert("key1", "val1");
        assertEquals("val1", tree.remove("key1"));
    }

    @Test
    public void testRandomInsertFind() {
        assertEquals(1, tree.nodeCount());
        int numKeys = 1000;
        Set<Integer> keys = new HashSet<>();
        for (int i = 0; i < numKeys; i++) {
            // Make range less than num keys so we're likely to get some duplicates
            int key = randomInt(numKeys / 2);
            keys.add(key);
            tree.insert(key, "val" + key);
            // Check invariants at each step to make sure still valid
            checkInvariants(tree);
        }
        assertEquals(numKeys, tree.keyCount());
        for (Integer i: keys) {
            Object val = tree.find(i);
            assertEquals("val" + i, val);
        }
    }

    @Test
    public void testSequentialInsertFind() {
        assertEquals(1, tree.nodeCount());
        int numKeys = 1000;
        for (int i = 0; i < numKeys; i++) {
            tree.insert(i, "val" + i);
            // Check invariants at each step to make sure still valid
            checkInvariants(tree);
        }
        assertEquals(numKeys, tree.keyCount());
        for (int i = 0; i < numKeys; i++) {
            Object val = tree.find(i);
            assertEquals("val" + i, val);
        }
    }

    private int randomInt(int max) {
        return randomInt(0, max);
    }

    private int randomInt(int min, int max) {
        return random.nextInt((max - min) + 1) + min;
    }

    protected void checkInvariants(BasicBplusTree tree) {
        new InvariantChecker().checkInvariants(tree);
    }

    protected class InvariantChecker {

        private int leafDepth = -1;

        void checkInvariants(BasicBplusTree tree) {
            assertTrue(tree.getRoot().isRoot());
            checkInvariants(tree.getRoot(), null, null, 0);
        }

        private void checkInvariants(BasicBplusTree.Node node, Comparable greaterOrEqualTo, Comparable lessThan,
                                     int depth) {

            if (node.isLeaf()) {
                assertTrue(node.numChildren() == 0);
            } else {
                assertTrue(node.numChildren() > 0);
            }

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
                //System.out.println("numkeys " + node.numKeys());
                assertTrue(node.numKeys() >=  B / 2 - 1);
                assertTrue(node.numKeys() <= B - 1);
            } else {
                //System.out.println("numkeys " + node.numKeys());
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
                    BasicBplusTree.Node leftChild = node.getChild(i - 1);
                    checkInvariants(leftChild, i == 1 ?  null : node.getKey(i - 1), node.getKey(i), depth + 1);
                }
                BasicBplusTree.Node rightChild = node.getChild(node.numKeys() - 1);
                checkInvariants(rightChild, node.getKey(node.numKeys() - 1), null, depth + 1);

            }

            // Check that parent-child relationship is consistent in both directions
            if (node.getParent() != null) {
                boolean found = false;
                for (int i = 0; i < node.getParent().numKeys(); i++) {
                    BasicBplusTree.Node child = node.getParent().getChild(i);
                    if (child == node) {
                        found = true;
                        break;
                    }
                }
                assertTrue(found);
            }

        }
    }


}
