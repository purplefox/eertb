package fox.btree;

import io.vertx.ext.unit.junit.Repeat;
import io.vertx.ext.unit.junit.RepeatRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertNotNull;

/**
 * Created by tim on 05/12/16.
 */
public class BasicBplusTreeTest {

    @Rule
    public RepeatRule repeatRule = new RepeatRule();

    private Random random = new SecureRandom();

    protected final int B = 4;

    protected BasicBplusTree tree;

    @Before
    public void setup() {
        tree = new BasicBplusTree(B);
    }

    @Test
    public void testEmptyTree() {
        assertEquals(1, tree.nodeCount());
        Node root = tree.getRoot();
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
    public void testOverwrite() {
        tree.insert("key1", "val1");
        assertEquals("val1", tree.find("key1"));
        tree.insert("key1", "val2");
        assertEquals("val2", tree.find("key1"));
        assertEquals(1, tree.keyCount());
    }

    @Test
    public void testRemove() {
        tree.insert("key1", "val1");
        assertEquals(1, tree.keyCount());
        assertEquals("val1", tree.remove("key1"));
        assertEquals(0, tree.keyCount());
    }

    @Test
    public void testRemoveNonExistent() {
        tree.insert("key1", "val1");
        assertEquals(1, tree.keyCount());
        assertEquals("val1", tree.remove("key1"));
        assertEquals(0, tree.keyCount());
        assertNull(tree.remove("key1"));
        assertEquals(0, tree.keyCount());
    }

    @Test
    //@Repeat(value=100000)
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
        assertEquals(keys.size(), tree.keyCount());
        for (Integer i : keys) {
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

    @Test
    //@Repeat(value=10000)
    public void testRandomInsertRemove() {
        assertEquals(1, tree.nodeCount());
        int numKeys = 1000;
        Set<Integer> keys = new HashSet<>();
        for (int i = 0; i < numKeys; i++) {
            int key = randomInt(numKeys);
            keys.add(key);
            tree.insert(key, "val" + key);
            checkInvariants(tree);
        }
        assertEquals(keys.size(), tree.keyCount());
        for (Integer i : keys) {
            Object val = tree.remove(i);
            assertEquals("val" + i, val);
            // Check invariants at each step to make sure still valid
            checkInvariants(tree);
        }
    }

    @Test
    public void testSequentialInsertRemove() {
        assertEquals(1, tree.nodeCount());
        int numKeys = 1000;
        for (int i = 0; i < numKeys; i++) {
            tree.insert(i, "val" + i);
        }
        assertEquals(numKeys, tree.keyCount());
        for (int i = 0; i < numKeys; i++) {
            Object val = tree.remove(i);
            checkInvariants(tree);
            assertEquals("val" + i, val);
        }
    }

    @Test
    public void testReverseSequentialInsertRemove() {
        assertEquals(1, tree.nodeCount());
        int numKeys = 1000;
        for (int i = 0; i < numKeys; i++) {
            tree.insert(i, "val" + i);
        }
        assertEquals(numKeys, tree.keyCount());
        for (int i = numKeys - 1; i >= 0; i--) {
            Object val = tree.remove(i);
            checkInvariants(tree);
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


}
