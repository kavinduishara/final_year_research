package org.example.rl;

import org.example.exec.ExecutionMetrics;
import org.example.plan.CutCandidate;
import org.example.qos.RewardCalculator;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Picks the cut with the best measured execution reward when every executable
 * candidate was observed during training on this query.
 */
public final class ObservedRewardSelector {

    private ObservedRewardSelector() {
    }

    public static CutSelection chooseIfFullyObserved(
            List<CutCandidate> candidates,
            Map<Integer, ExecutionMetrics> metricsByCutNodeId
    ) {
        List<CutCandidate> executable =
                candidates.stream()
                        .filter(CutCandidate::executable)
                        .toList();

        if (executable.isEmpty()) {
            return null;
        }

        boolean allObserved =
                executable.stream()
                        .allMatch(candidate ->
                                metricsByCutNodeId
                                        .containsKey(
                                                candidate.nodeId()
                                        )
                        );

        if (!allObserved) {
            return null;
        }

        CutCandidate bestCandidate =
                executable.stream()
                        .max(Comparator
                                .comparingDouble(
                                        (CutCandidate candidate) ->
                                                RewardCalculator
                                                        .executionReward(
                                                                metricsByCutNodeId
                                                                        .get(
                                                                                candidate
                                                                                        .nodeId()
                                                                        )
                                                        )
                                )
                                .thenComparingDouble(
                                        candidate ->
                                                -candidate
                                                        .totalTransferCost()
                                )
                                .thenComparingInt(
                                        CutCandidate::nodeId
                                ))
                        .orElseThrow();

        double bestReward =
                RewardCalculator.executionReward(
                        metricsByCutNodeId.get(
                                bestCandidate.nodeId()
                        )
                );

        return new CutSelection(
                new Action(
                        bestCandidate.nodeId()
                ),
                bestCandidate,
                CutSelection.SelectionReason
                        .EXECUTION_BEST,
                bestReward
        );
    }
}
