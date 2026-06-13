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
        FALLBACK_CHEAPEST_TRANSFER
    }
}
