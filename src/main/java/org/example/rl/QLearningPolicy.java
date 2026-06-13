package org.example.rl;

import org.example.plan.CutCandidate;

import java.util.List;

public class QLearningPolicy {

    private final QTable qTable;

    public QLearningPolicy(QTable qTable) {
        this.qTable = qTable;
    }

    public Action choose(List<CutCandidate> candidates) {
        return CutSelector.choose(
                candidates,
                qTable
        ).action();
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
