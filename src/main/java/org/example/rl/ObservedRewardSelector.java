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
 *
 * <p>After Q1 training executes both cut@38 and cut@42, inference can skip
 * LinUCB and directly pick the cut with highest {@link RewardCalculator} score.
 *
 * <p>Example:
 * <pre>
 *   metrics = {38 → totalMs=2100, 42 → totalMs=3200}
 *   chooseIfFullyObserved(candidates, metrics)
 *     → CutSelection(Action(38), reason=EXECUTION_BEST, score=…)
 * </pre>
 */
public final class ObservedRewardSelector {

    private ObservedRewardSelector() {
    }

    /**
     * Returns the best observed cut, or {@code null} if not all executable cuts were measured.
     *
     * @param candidates         Q1 cuts with executability flags
     * @param metricsByCutNodeId training results keyed by node id (38, 42)
     * @return selection with EXECUTION_BEST reason, or null to fall back to LinUCB
     */
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
