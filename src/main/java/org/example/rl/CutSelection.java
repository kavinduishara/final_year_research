package org.example.rl;

import org.example.plan.CutCandidate;

/**
 * Result of choosing which JOIN cut to execute.
 *
 * <p>Example:
 * <pre>
 *   action    = Action(42)
 *   candidate = CutCandidate(nodeId=42, fragment1Worker="worker2", locality="INTERMEDIATE_ONLY", ...)
 *   reason    = LINUCB
 *   bestScore = 0.73   (UCB score from contextual bandit)
 * </pre>
 */
public record CutSelection(
        /** Which cut node to execute. */
        Action action,
        /** Full enriched candidate metadata for that cut. */
        CutCandidate candidate,
        /** How the cut was chosen. */
        SelectionReason reason,
        /** Bandit UCB score, or 0.0 for fallback. */
        double bestScore
) {
    public enum SelectionReason {
        /** LinUCB model picked highest UCB among executable cuts. */
        LINUCB,
        /** After training, all cuts were executed — pick best observed reward. */
        EXECUTION_BEST,
        /** No model data — pick lowest estimated totalTransferCost. */
        FALLBACK_CHEAPEST_TRANSFER
    }
}
