package org.example.rl;

import org.apache.calcite.rel.RelNode;

import java.util.List;

/**
 * @deprecated Use {@link BaselinePolicyFactory} with benchmark.baseline=deep-join.
 */
@Deprecated
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
