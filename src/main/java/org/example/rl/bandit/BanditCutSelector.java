package org.example.rl.bandit;

import org.example.config.ResearchSettings;
import org.example.plan.CutCandidate;
import org.example.rl.Action;
import org.example.rl.CutSelection;

import java.util.Comparator;
import java.util.List;

/**
 * LinUCB-based cut selector: scores each executable arm by UCB upper bound.
 *
 * <p>Before enough observations, falls back to cheapest-transfer cut (like static QoS).
 * After training on Q1 cuts 38 and 42, prefers the arm with highest
 * {@code mean_reward + α × uncertainty}.
 *
 * <p>Example:
 * <pre>
 *   observations &lt; min → Action(38) via FALLBACK_CHEAPEST_TRANSFER
 *   observations ≥ min → Action(38) via LINUCB if shallow cut scored higher
 * </pre>
 */
public final class BanditCutSelector {

    private BanditCutSelector() {
    }

    /**
     * Picks the best cut using the contextual bandit model.
     *
     * @param candidates enriched Q1 cuts [38, 42]
     * @param bandit     trained LinUCB model
     * @return {@link CutSelection} with reason LINUCB or FALLBACK_CHEAPEST_TRANSFER
     */
    public static CutSelection choose(
            List<CutCandidate> candidates,
            ContextualBandit bandit
    ) {
        List<CutCandidate> executable =
                candidates.stream()
                        .filter(CutCandidate::executable)
                        .toList();

        if (executable.isEmpty()) {
            throw new IllegalStateException(
                    "No executable cut candidate found"
            );
        }

        if (bandit.observations()
                < ResearchSettings.banditMinObservations()) {
            return fallbackCheapest(executable);
        }

        CutCandidate bestCandidate = null;
        double bestScore =
                Double.NEGATIVE_INFINITY;

        for (CutCandidate candidate : executable) {
            double score =
                    bandit.score(
                            CutFeatures.from(candidate)
                    );

            if (score > bestScore
                    || (score == bestScore
                    && bestCandidate != null
                    && candidate.totalTransferCost()
                    < bestCandidate.totalTransferCost())) {

                bestScore = score;
                bestCandidate = candidate;
            }
        }

        return new CutSelection(
                new Action(
                        bestCandidate.nodeId()
                ),
                bestCandidate,
                CutSelection.SelectionReason.LINUCB,
                bestScore
        );
    }

    /**
     * Cold-start policy: minimum total transfer (matches static QoS intent).
     *
     * @param executable runnable cuts only
     * @return cheapest-transfer selection with score 0
     */
    private static CutSelection fallbackCheapest(
            List<CutCandidate> executable
    ) {
        CutCandidate cheapest =
                executable.stream()
                        .min(Comparator
                                .comparingDouble(
                                        CutCandidate::totalTransferCost
                                )
                                .thenComparingInt(
                                        CutCandidate::nodeId
                                ))
                        .orElseThrow();

        return new CutSelection(
                new Action(
                        cheapest.nodeId()
                ),
                cheapest,
                CutSelection.SelectionReason
                        .FALLBACK_CHEAPEST_TRANSFER,
                0.0
        );
    }
}
