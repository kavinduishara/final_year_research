package org.example.rl;

import org.example.plan.CutCandidate;

import java.util.Comparator;
import java.util.List;

/**
 * Baseline that over-estimates intermediate size (ignores join selectivity).
 *
 * <p>Uses {@link org.example.qos.BaselineCostEstimator} pessimistic row counts.
 * On Q1, shallow cut@38 (customer⋈orders) looks expensive because intermediate
 * rows are inflated toward orders cardinality — often pushing the baseline to cut@42.
 *
 * <p>Example:
 * <pre>
 *   cut@38 baselineDecisionCost ≈ ship(lineitem) + estimate(1.5M rows)
 *   cut@42 baselineDecisionCost ≈ 0 intermediate (same-worker deep cut)
 *   choose() → Action(42)
 * </pre>
 */
public class PessimisticQoSBaselinePolicy implements CutPolicy {

    /**
     * @param candidates enriched executable cuts for Q1
     * @return action with minimum {@link CutCandidate#baselineDecisionCost()}
     */
    @Override
    public Action choose(
            List<CutCandidate> candidates
    ) {
        CutCandidate best =
                candidates.stream()
                        .filter(CutCandidate::executable)
                        .min(Comparator
                                .comparingDouble(
                                        CutCandidate::baselineDecisionCost
                                )
                                .thenComparingInt(
                                        CutCandidate::nodeId
                                ))
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "No executable cut for pessimistic QoS baseline"
                                ));

        return new Action(
                best.nodeId()
        );
    }
}
