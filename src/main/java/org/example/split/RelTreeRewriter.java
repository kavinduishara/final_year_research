package org.example.split;

import org.apache.calcite.rel.RelNode;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Generic immutable RelNode tree rewriter: replace one subtree with another.
 *
 * <p>Used when cutting Q1 at Join#42: the deep join subtree is replaced by a
 * {@link org.apache.calcite.rel.logical.LogicalTableScan} over
 * {@code inter_single_n42} so fragment2 can reference materialized rows.
 *
 * <p>Example:
 * <pre>
 *   replace(root, Join#42, IntermediateScan("inter_single_n42"))
 *   → new plan where Join#42 subtree is the scan node
 * </pre>
 */
public class RelTreeRewriter {

    /**
     * Returns a copy of {@code root} with {@code target} swapped for {@code replacement}.
     *
     * @param root        full Q1 plan
     * @param target      node to replace, e.g. Join#42
     * @param replacement intermediate table scan or other substitute
     * @return new root (unchanged reference if no match found in subtree)
     */
    public static RelNode replace(RelNode root, RelNode target, RelNode replacement) {
        if (root == target) return replacement;

        List<RelNode> inputs = root.getInputs();
        if (inputs == null || inputs.isEmpty()) return root;

        List<RelNode> newInputs = inputs.stream()
                .map(in -> replace(in, target, replacement))
                .collect(Collectors.toList());

        // If nothing changed, avoid copying
        boolean changed = false;
        for (int i = 0; i < inputs.size(); i++) {
            if (inputs.get(i) != newInputs.get(i)) { changed = true; break; }
        }
        if (!changed) return root;

        return root.copy(root.getTraitSet(), newInputs);
    }
}
