package org.example.benchmark;

import org.example.exec.ExecutionMetrics;
import org.example.plan.CutCandidate;
import org.example.rl.Action;
import org.example.rl.BaselinePolicyFactory;

import java.util.List;
import java.util.Map;

/**
 * Converts execution outcomes into comparable {@link BenchmarkResult} records.
 *
 * <p>After Q1 training, each cut (38, 42) has measured {@link ExecutionMetrics}.
 * This class picks the baseline vs learned cut and packages timings for the summary table.
 *
 * <p>Example:
 * <pre>
 *   runBaseline(candidates, cutPoints, metrics)
 *     → baseline picks cut@42 (WeightedShip), totalTimeMs=3200
 *   runLearnedPolicy(action(38), …)
 *     → LinUCB cut@38, totalTimeMs=2100
 * </pre>
 */
public class BenchmarkRunner {

    /**
     * @param policyName display label, e.g. "WeightedShip" or "LinUCB"
     * @param metrics    measured times for the chosen cut
     * @return benchmark row with reward input = runtime + transfer
     */
    public static BenchmarkResult fromExecutionMetrics(
            String policyName,
            ExecutionMetrics metrics
    ) {
        return new BenchmarkResult(
                policyName,
                metrics.runtimeMs()
                        + metrics.transferTimeMs(),
                metrics.runtimeMs(),
                metrics.transferTimeMs(),
                metrics.totalTimeMs(),
                metrics.baseTableShipTimeMs(),
                metrics.intermediateTransferTimeMs()
        );
    }

    /**
     * Runs the configured static baseline policy and looks up its metrics.
     *
     * @param candidates          enriched Q1 cuts [38, 42]
     * @param cutPoints           raw JOIN RelNodes
     * @param metricsByCutNodeId  training measurements, e.g. {38→metrics, 42→metrics}
     * @return baseline {@link BenchmarkResult}
     */
    public static BenchmarkResult runBaseline(
            List<CutCandidate> candidates,
            List<org.apache.calcite.rel.RelNode> cutPoints,
            Map<Integer, ExecutionMetrics> metricsByCutNodeId
    ) {
        Action action =
                BaselinePolicyFactory.choose(
                        candidates,
                        cutPoints
                );

        return metricsForAction(
                BaselinePolicyFactory.displayName(),
                action,
                candidates,
                metricsByCutNodeId
        );
    }

    /**
     * Packages metrics for the LinUCB (or other learned) chosen cut.
     *
     * @param learnedAction       e.g. Action(38) after training on Q1
     * @param candidates          all executable cuts
     * @param metricsByCutNodeId  observed execution times per cut
     * @return learned-policy benchmark result
     */
    public static BenchmarkResult runLearnedPolicy(
            Action learnedAction,
            List<CutCandidate> candidates,
            Map<Integer, ExecutionMetrics> metricsByCutNodeId
    ) {
        return metricsForAction(
                "LinUCB",
                learnedAction,
                candidates,
                metricsByCutNodeId
        );
    }

    /**
     * Resolves cut candidate and metrics for one {@link Action}.
     *
     * @throws IllegalStateException if cut is not executable or metrics missing
     */
    private static BenchmarkResult metricsForAction(
            String name,
            Action action,
            List<CutCandidate> candidates,
            Map<Integer, ExecutionMetrics> metricsByCutNodeId
    ) {
        CutCandidate cut =
                candidates.stream()
                        .filter(c ->
                                c.nodeId()
                                        == action.cutNodeId()
                        )
                        .findFirst()
                        .orElseThrow();

        if (!cut.executable()) {
            throw new IllegalStateException(
                    "Cut "
                            + action.cutNodeId()
                            + " is not executable for "
                            + name
            );
        }

        ExecutionMetrics metrics =
                metricsByCutNodeId.get(
                        action.cutNodeId()
                );

        if (metrics == null) {
            throw new IllegalStateException(
                    "No execution metrics for cut "
                            + action.cutNodeId()
            );
        }

        return fromExecutionMetrics(
                name,
                metrics
        );
    }
}
