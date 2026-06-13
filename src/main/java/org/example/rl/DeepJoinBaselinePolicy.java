package org.example.rl;

import org.apache.calcite.rel.RelNode;

import java.util.List;

/**
 * Weak baseline for research: always cut at the first (root) join in the plan.
 * Often requires shipping large base tables and is expected to lose to RL.
 */
public class DeepJoinBaselinePolicy implements Policy {

    @Override
    public Action choose(List<RelNode> legalCutPoints) {

        if (legalCutPoints == null || legalCutPoints.isEmpty()) {
            throw new IllegalArgumentException(
                    "No legal cut points."
            );
        }

        RelNode chosen =
                legalCutPoints.get(0);

        return new Action(
                chosen.getId()
        );
    }
}
