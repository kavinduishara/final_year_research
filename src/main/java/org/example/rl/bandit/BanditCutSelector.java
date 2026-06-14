package org.example.rl.bandit;

import org.example.config.ResearchSettings;
import org.example.plan.CutCandidate;
import org.example.rl.Action;
import org.example.rl.CutSelection;

import java.util.Comparator;
import java.util.List;

public final class BanditCutSelector {

    private BanditCutSelector() {
    }

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
