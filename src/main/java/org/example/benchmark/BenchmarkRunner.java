package org.example.benchmark;

import org.example.plan.CutCandidate;
import org.example.rl.Action;

import java.util.List;

public class BenchmarkRunner {

    public static BenchmarkResult runBaseline(
            List<CutCandidate> candidates,
            List<org.apache.calcite.rel.RelNode> cutPoints) {

        Action action =
                firstExecutableAction(
                        candidates,
                        cutPoints
                );

        double totalTransfer = 0;

        for (int i = 0; i < 100; i++) {
            CutCandidate chosen =
                    findCandidate(
                            candidates,
                            action.cutNodeId()
                    );

            totalTransfer +=
                    chosen.totalTransferCost();
        }

        return new BenchmarkResult(
                "Baseline",
                totalTransfer / 100.0
        );
    }

    private static Action firstExecutableAction(
            List<CutCandidate> candidates,
            List<org.apache.calcite.rel.RelNode> cutPoints
    ) {
        for (org.apache.calcite.rel.RelNode node : cutPoints) {
            CutCandidate candidate =
                    findCandidate(
                            candidates,
                            node.getId()
                    );

            if (candidate.executable()) {
                return new Action(
                        candidate.nodeId()
                );
            }
        }

        throw new IllegalStateException(
                "No executable cut point found"
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
                    chosen.totalTransferCost();
        }

        return new BenchmarkResult(
                "QLearning",
                totalTransfer / 100.0
        );
    }
}
