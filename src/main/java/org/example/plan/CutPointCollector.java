package org.example.plan;

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelVisitor;
import org.apache.calcite.rel.logical.LogicalJoin;

import java.util.ArrayList;
import java.util.List;

/**
 * Finds legal cut points in a Calcite logical plan.
 *
 * <p>Policy: only {@link LogicalJoin} nodes can be cut (V0).
 *
 * <p>Example Q1 plan (simplified):
 * <pre>
 *   LogicalAggregate
 *     └── LogicalJoin (customer ⋈ orders)     ← cut candidate id=38
 *           └── LogicalJoin (⋯ ⋈ lineitem)    ← cut candidate id=42
 * </pre>
 * Returns both JOIN RelNodes; each becomes a {@link CutCandidate}.
 */
public class CutPointCollector {

    /**
     * Walks the RelNode tree depth-first and collects every LogicalJoin.
     *
     * @param root best plan from BestPlanFinder (e.g. Q1 aggregate root)
     * @return list of JOIN RelNodes, e.g. [Join#38, Join#42] (order = visit order)
     */
    public static List<RelNode> collectJoinCuts(RelNode root) {
        List<RelNode> cuts = new ArrayList<>();

        new RelVisitor() {
            @Override
            public void visit(RelNode node, int ordinal, RelNode parent) {
                if (node instanceof LogicalJoin) {
                    cuts.add(node);
                }
                super.visit(node, ordinal, parent);
            }
        }.go(root);

        return cuts;
    }
}
