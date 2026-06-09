package org.example.rl;

import org.example.plan.CutCandidate;

import java.util.List;

public class QLearningPolicy {

    private final QTable qTable;

    public QLearningPolicy(QTable qTable) {
        this.qTable = qTable;
    }

    public Action choose(List<CutCandidate> candidates) {

        CutCandidate bestCandidate = null;
        double bestQ = Double.NEGATIVE_INFINITY;

        for (CutCandidate candidate : candidates) {

            State state =
                    StateBuilder.from(candidate);

            String key =
                    state.rowBucket()
                            + "_"
                            + state.depthBucket()
                            + "_"
                            + state.costBucket()
                            + "_"
                            + candidate.nodeId();

            double q =
                    qTable.get(key);

            if (q > bestQ) {
                bestQ = q;
                bestCandidate = candidate;
            }
        }

        if (bestCandidate == null) {
            throw new IllegalStateException(
                    "No candidate selected"
            );
        }

        return new Action(
                bestCandidate.nodeId()
        );
    }
}