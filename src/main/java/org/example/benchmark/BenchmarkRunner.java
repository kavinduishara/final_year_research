package org.example.benchmark;

import org.example.exec.ExecutionMetrics;
import org.example.plan.CutCandidate;
import org.example.rl.Action;
import org.example.rl.DeepJoinBaselinePolicy;
import org.example.rl.Policy;

import java.util.List;
import java.util.Map;

public class BenchmarkRunner {

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

    public static BenchmarkResult runDeepJoinBaseline(
            List<CutCandidate> candidates,
            List<org.apache.calcite.rel.RelNode> cutPoints,
            Map<Integer, ExecutionMetrics> metricsByCutNodeId
    ) {
        Policy policy =
                new DeepJoinBaselinePolicy();

        Action action =
                policy.choose(cutPoints);

        return metricsForAction(
                "DeepJoinBaseline",
                action,
                candidates,
                metricsByCutNodeId
        );
    }

    public static BenchmarkResult runQLearning(
            Action learnedAction,
            List<CutCandidate> candidates,
            Map<Integer, ExecutionMetrics> metricsByCutNodeId
    ) {
        return metricsForAction(
                "QLearning",
                learnedAction,
                candidates,
                metricsByCutNodeId
        );
    }

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
