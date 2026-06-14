package org.example.rl;

import org.example.plan.CutCandidate;

public record CutSelection(
        Action action,
        CutCandidate candidate,
        SelectionReason reason,
        double bestQ
) {
    public enum SelectionReason {
        RL,
        BANDIT_LINUCB,
        BANDIT_THOMPSON,
        EXECUTION_BEST,
        FALLBACK_CHEAPEST_TRANSFER
    }
}
