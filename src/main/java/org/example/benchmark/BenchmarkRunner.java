package org.example.benchmark;

import org.example.plan.CutCandidate;
import org.example.qos.TransferCostEstimator;
import org.example.rl.Action;
import org.example.rl.BaselinePolicy;
import org.example.rl.Policy;

import java.util.List;

public class BenchmarkRunner {

    public static BenchmarkResult runBaseline(
            List<CutCandidate> candidates,
            List<org.apache.calcite.rel.RelNode> cutPoints) {

        Policy policy = new BaselinePolicy();

        double totalTransfer = 0;

        for (int i = 0; i < 100; i++) {

            Action action =
                    policy.choose(cutPoints);

            CutCandidate chosen =
                    findCandidate(
                            candidates,
                            action.cutNodeId()
                    );

            totalTransfer +=
                    TransferCostEstimator.estimate(
                            chosen.estimatedRows()
                    );
        }

        return new BenchmarkResult(
                "Baseline",
                totalTransfer / 100.0
        );
    }

    private static CutCandidate findCandidate(
            List<CutCandidate> candidates,
            int nodeId) {

        return candidates.stream()
                .filter(c -> c.nodeId() == nodeId)
                .findFirst()
                .orElseThrow();
    }
    public static BenchmarkResult runQLearning(
            List<CutCandidate> candidates,
            Action learnedAction) {

        double totalTransfer = 0;

        for (int i = 0; i < 100; i++) {

            CutCandidate chosen =
                    findCandidate(
                            candidates,
                            learnedAction.cutNodeId()
                    );

            totalTransfer +=
                    TransferCostEstimator.estimate(
                            chosen.estimatedRows()
                    );
        }

        return new BenchmarkResult(
                "QLearning",
                totalTransfer / 100.0
        );
    }
}