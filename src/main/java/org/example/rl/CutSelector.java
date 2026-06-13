package org.example.rl;

import org.example.plan.CutCandidate;

import java.util.Comparator;
import java.util.List;

public final class CutSelector {

    private CutSelector() {
    }

    public static CutSelection choose(
            List<CutCandidate> candidates,
            QTable qTable
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

        boolean anyKnown =
                executable.stream()
                        .anyMatch(candidate ->
                                qTable.contains(
                                        stateKeyFor(
                                                candidate
                                        )
                                )
                        );

        if (!anyKnown) {
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

        CutCandidate bestCandidate = null;
        double bestQ =
                Double.NEGATIVE_INFINITY;

        for (CutCandidate candidate : executable) {

            String key =
                    stateKeyFor(candidate);

            double q =
                    qTable.get(key);

            if (q > bestQ
                    || (q == bestQ
                    && bestCandidate != null
                    && candidate.totalTransferCost()
                    < bestCandidate.totalTransferCost())) {

                bestQ = q;
                bestCandidate = candidate;
            }
        }

        return new CutSelection(
                new Action(
                        bestCandidate.nodeId()
                ),
                bestCandidate,
                CutSelection.SelectionReason.RL,
                bestQ
        );
    }

    private static String stateKeyFor(
            CutCandidate candidate
    ) {
        State state =
                StateBuilder.from(candidate);

        return QLearningPolicy.stateKey(
                state,
                candidate.nodeId()
        );
    }
}
