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

    /**
     * State-only key so Q-values generalize across queries
     * (Calcite node ids differ per plan).
     */
    public static String stateKey(State state) {
        return state.rowBucket()
                + "_"
                + state.depthBucket()
                + "_"
                + state.costBucket()
                + "_"
                + state.localityBucket();
    }
}
