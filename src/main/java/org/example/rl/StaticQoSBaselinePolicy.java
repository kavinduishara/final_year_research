package org.example.rl;

import org.example.plan.CutCandidate;

import java.util.Comparator;
import java.util.List;

/**
 * Traditional static QoS baseline: pick the executable cut with the lowest
 * estimated transfer cost (base-table shipping + intermediate transfer).
 *
 * <p>Models a cost-based distributed optimizer that uses statistics but
 * does not learn from execution. On Q1 with worker1/worker2, shallow cut@38
 * often wins because it avoids shipping the 6M-row lineitem table.
 *
 * <p>Example:
 * <pre>
 *   cut@38 totalTransferCost ≈ 4_500_000 (intermediate only)
 *   cut@42 totalTransferCost ≈ 600_000_000 (ship lineitem to worker1)
 *   choose() → Action(38)
 * </pre>
 */
public class StaticQoSBaselinePolicy implements CutPolicy {

    /**
     * @param candidates enriched cuts for the query
     * @return action with minimum {@link CutCandidate#totalTransferCost()}
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
                                        CutCandidate::totalTransferCost
                                )
                                .thenComparingDouble(
                                        CutCandidate::estimatedCost
                                )
                                .thenComparingInt(
                                        CutCandidate::nodeId
                                ))
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "No executable cut for static QoS baseline"
                                ));

        return new Action(
                best.nodeId()
        );
    }
}
