package org.example.rl;

import org.example.plan.CutCandidate;

public record CutSelection(
        Action action,
        CutCandidate candidate,
        SelectionReason reason,
        double bestScore
) {
    public enum SelectionReason {
        LINUCB,
        EXECUTION_BEST,
        FALLBACK_CHEAPEST_TRANSFER
    }
}
