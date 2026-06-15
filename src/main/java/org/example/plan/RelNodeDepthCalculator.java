package org.example.plan;

import org.apache.calcite.rel.RelNode;

/**
 * Computes depth of a target node relative to the plan root.
 *
 * <p>Depth = number of edges from root to target (root itself is depth 0).
 *
 * <p>Example Q1 plan:
 * <pre>
 *   LogicalAggregate (depth 0)
 *     └── LogicalJoin#42 (depth 1)
 *           └── LogicalJoin#38 (depth 2)
 * </pre>
 * {@code depth(root, Join#38)} → 2; used to prefer deeper cuts in baselines.
 */
public class RelNodeDepthCalculator {

    /**
     * @param root   plan root (aggregate for Q1)
     * @param target cut node, e.g. Join#42
     * @return depth from root, or -1 if target is not in the tree
     */
    public static int depth(RelNode root, RelNode target) {
        return depth(root, target, 0);
    }

    /**
     * Recursive DFS helper.
     *
     * @param current      current node in traversal
     * @param target       node to find
     * @param currentDepth depth of {@code current}
     * @return depth of target, or -1 if not found in this branch
     */
    private static int depth(RelNode current,
                             RelNode target,
                             int currentDepth) {

        if (current == target) {
            return currentDepth;
        }

        for (RelNode input : current.getInputs()) {
            int found =
                    depth(input,
                            target,
                            currentDepth + 1);

            if (found != -1) {
                return found;
            }
        }

        return -1;
    }
}
