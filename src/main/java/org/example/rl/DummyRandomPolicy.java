package org.example.rl;

import org.apache.calcite.rel.RelNode;

import java.util.List;
import java.util.Random;

public class DummyRandomPolicy implements Policy {

    private final Random random = new Random();

    @Override
    public Action choose(List<RelNode> legalCutPoints) {
        if (legalCutPoints == null || legalCutPoints.isEmpty()) {
            throw new IllegalArgumentException("No legal cut points available.");
        }
        RelNode pick = legalCutPoints.get(random.nextInt(legalCutPoints.size()));
        return new Action(pick.getId());
    }
}
