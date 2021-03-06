package fox.btree;

/**
 * Created by tim on 08/12/16.
 */
public class InvariantChecker {

    private int branchingFactor;
    private int leafDepth = -1;

    void checkInvariants(BasicBplusTree tree) {
        leafDepth = -1;
        branchingFactor = tree.branchingFactor();
        assertTrue("root node must be a root", tree.getRoot().isRoot());
        checkInvariants(tree.getRoot(), null, null, 0);
    }

    private void checkInvariants(Node node, Comparable greaterThanOrEqual, Comparable lessThan, int depth) {

        if (node.isLeaf()) {
            assertTrue("leaft node must not have children", node.numChildren() == 0);
        } else {
            assertTrue("internal node must have children", node.numChildren() > 0);
        }

        // Check that parent-child relationship is consistent in both directions
        if (node.getParent() != null) {
            boolean found = false;
            for (int i = 0; i < node.getParent().numKeys(); i++) {
                Node child = node.getParent().getChild(i);
                if (child == node) {
                    found = true;
                    break;
                }
            }
            assertTrue("parent child relationship inconsistent in node " + node.getNodeNum(), found);
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
                assertTrue("leaf root node num keys must be >= 0", node.numKeys() >= 0);
                assertTrue("leaft root node num keys must be <= B - 1", node.numKeys() <= branchingFactor - 1);
            } else {
                assertTrue("non leaf root node num keys must be >= 2", node.numKeys() >= 2);
                assertTrue("non leaf root node num keys must be <= B", node.numKeys() <= branchingFactor);
            }
        } else if (node.isLeaf()) {
            assertTrue("leaf node num keys must be >= B / 2 - 1", node.numKeys() >= branchingFactor / 2 - 1);
            assertTrue("leaf node num keys must be <= B - 1", node.numKeys() <= branchingFactor - 1);
        } else {
            assertTrue("internal node num keys must be >= B / 2", node.numKeys() >= branchingFactor / 2);
            assertTrue("internal node num keys must be <= B", node.numKeys() <= branchingFactor);
        }

        // The left most key in any leftmost internal node is never used so we don't have to keep it matching the
        // left most key in its child, so we only check from 1 in this case, otherwise zero
        int start = 1;
        Node par = node.getParent();
        while (par != null) {
            if (node.getParent().getChild(0) != node) {
                // Not left most node
                start = 0;
                break;
            }
            par = par.getParent();
        }

        Comparable prev = null;
        for (int i = start; i < node.numKeys(); i++) {
            Comparable key = node.getKey(i);

            // Invariant: key range
            if (greaterThanOrEqual != null) {
                assertTrue("key:" + key + " not >=" + greaterThanOrEqual, key.compareTo(greaterThanOrEqual) >= 0);
            }
            if (lessThan != null) {
                assertTrue("key:" + key + " lt:" + lessThan, key.compareTo(lessThan) < 0);
            }

            // Check the values are correct for a leaf
            if (node.isLeaf()) {
                Object val = node.getValue(i);
                assertEquals("val" + key, val);
            }

            // Invariant: keys must be in sorted order
            if (prev != null) {
                assertTrue("keys not in sorted order in node " + node.getNodeNum(), key.compareTo(prev) >= 0);
            }
            prev = key;

        }

        // Recurse
        if (!node.isLeaf()) {
            for (int i = 0; i < node.numKeys(); i++) {
                Node child = node.getChild(i);
                Comparable gOrE = i == 0 ? null : node.getKey(i);
                Comparable less = i < node.numKeys() - 1 ? node.getKey(i + 1) : null;
                checkInvariants(child, gOrE, less, depth + 1);
            }
            Node rightChild = node.getChild(node.numKeys() - 1);
            checkInvariants(rightChild, node.getKey(node.numKeys() - 1), null, depth + 1);

        }

    }

    private void assertTrue(String msg, boolean ok) {
        if (!ok) {
            throw new IllegalStateException(msg);
        }
    }

    private void assertEquals(Object object1, Object object2) {
        if (!object1.equals(object2)) {
            throw new IllegalStateException("Not equal " + object1 + " " + object2);
        }
    }
}
