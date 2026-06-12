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

            if (!candidate.executable()) {
                continue;
            }

            State state =
                    StateBuilder.from(candidate);

            String key =
                    stateKey(
                            state,
                            candidate.nodeId()
                    );

            double q =
                    qTable.get(key);

            if (q > bestQ) {
                bestQ = q;
                bestCandidate = candidate;
            }
        }

        if (bestCandidate == null) {
            throw new IllegalStateException(
                    "No executable candidate selected"
            );
        }

        return new Action(
                bestCandidate.nodeId()
        );
    }

    public static String stateKey(
            State state,
            int nodeId
    ) {
        return state.rowBucket()
                + "_"
                + state.depthBucket()
                + "_"
                + state.costBucket()
                + "_"
                + state.localityBucket()
                + "_"
                + nodeId;
    }
}
