package org.example.rl;

import org.apache.calcite.rel.RelNode;

import java.util.List;

public class BaselinePolicy implements Policy {

    @Override
    public Action choose(List<RelNode> legalCutPoints) {

        if (legalCutPoints == null || legalCutPoints.isEmpty()) {
            throw new IllegalArgumentException("No legal cut points.");
        }

        // Always choose first join
        RelNode chosen = legalCutPoints.get(0);

        return new Action(chosen.getId());
    }
}