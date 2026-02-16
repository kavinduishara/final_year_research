package org.example.plan;

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelVisitor;
import org.apache.calcite.rel.logical.LogicalJoin;

import java.util.ArrayList;
import java.util.List;

public class CutPointCollector {

    /**
     * V0 policy: Only JOIN nodes are legal cut points.
     * This keeps splitting simple + meaningful.
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
