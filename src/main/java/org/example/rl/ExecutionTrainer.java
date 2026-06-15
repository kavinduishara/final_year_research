package org.example.rl;

import org.apache.calcite.rel.RelNode;
import org.example.calcite.CalciteContext;
import org.example.config.ResearchSettings;
import org.example.exec.ExecutionMetrics;
import org.example.experiment.DistributedQueryRunner;
import org.example.experiment.ExecutionSession;
import org.example.plan.CutCandidate;
import org.example.qos.RewardCalculator;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Trains LinUCB by actually executing every executable cut and observing rewards.
 *
 * <p>Example Q1 with cuts [38, 42], episodes=2:
 * <pre>
 *   Episode 1: execute cut 38 → reward -2800 → observe()
 *              execute cut 42 → reward -3400 → observe()
 *   Episode 2: (repeat)
 *   metricsByCutNodeId = {38→ExecutionMetrics(...), 42→ExecutionMetrics(...)}
 * </pre>
 * Cuts sorted by totalTransferCost (cheapest first) to reduce training cost.
 */
public class ExecutionTrainer {

    /**
     * @param policyEngine trained/updated bandit model
     * @param metricsByCutNodeId cached real execution times per cut node id
     */
    public record TrainingResult(
            CutPolicyEngine policyEngine,
            Map<Integer, ExecutionMetrics> metricsByCutNodeId
    ) {
    }

    /**
     * Train with fresh bandit; saves bandit.json unless benchmark/workload-train mode.
     */
    public static TrainingResult train(
            RelNode bestPlan,
            CalciteContext ctx,
            List<CutCandidate> candidates,
            ExecutionSession session
    ) throws Exception {
        return train(
                bestPlan,
                ctx,
                candidates,
                session,
                CutPolicyEngine.createFresh(),
                shouldPersistAfterTrain()
        );
    }

    /**
     * Full training loop: for each episode, execute each executable cut, observe reward.
     *
     * @param bestPlan      Q1 RelNode
     * @param ctx           Calcite context
     * @param candidates    enriched cuts from QueryService.prepare()
     * @param session       e.g. single() or forQuery(3) for Q3 in benchmark
     * @param policyEngine  shared bandit (benchmark uses one across all 10 queries)
     * @param persistModel  false in benchmark (save once at end)
     * @return updated policyEngine + map of cut node id → last execution metrics
     */
    public static TrainingResult train(
            RelNode bestPlan,
            CalciteContext ctx,
            List<CutCandidate> candidates,
            ExecutionSession session,
            CutPolicyEngine policyEngine,
            boolean persistModel
    ) throws Exception {

        int episodes =
                ResearchSettings.trainingEpisodes();

        Map<Integer, ExecutionMetrics> metricsByCutNodeId =
                new HashMap<>();

        // e.g. [42, 38] if cut 42 has lower estimated transfer
        List<CutCandidate> ordered =
                candidates.stream()
                        .filter(CutCandidate::executable)
                        .sorted(Comparator.comparingDouble(
                                CutCandidate::totalTransferCost
                        ))
                        .toList();

        System.out.println(
                "\n===== EXECUTION-BASED TRAINING ====="
        );

        System.out.println(
                "Mode       = "
                        + ResearchSettings.trainingMode()
        );

        System.out.println(
                "Algorithm  = "
                        + policyEngine.algorithmLabel()
        );

        System.out.println(
                "Episodes   = "
                        + episodes
        );

        System.out.println(
                "Cut order (cheap first) = "
                        + ordered.stream()
                        .map(CutCandidate::nodeId)
                        .toList()
        );

        System.out.println(
                "Session    = "
                        + session.id()
        );

        System.out.println(
                "Tip: set training.mode=inference after training for fast queries."
        );
        System.out.flush();

        for (int episode = 1; episode <= episodes; episode++) {

            System.out.println(
                    "\n----- Episode "
                            + episode
                            + " / "
                            + episodes
                            + " -----"
            );
            System.out.flush();

            for (CutCandidate candidate : ordered) {

                Action action =
                        new Action(
                                candidate.nodeId()
                        );

                System.out.println(
                        "\nTraining cut node "
                                + candidate.nodeId()
                                + " (episode "
                                + episode
                                + ")"
                );
                System.out.flush();

                ExecutionMetrics metrics =
                        DistributedQueryRunner.execute(
                                bestPlan,
                                action,
                                ctx,
                                candidate,
                                session,
                                episode == 1
                                        && candidate
                                        == ordered.get(0)
                        ).metrics();

                metricsByCutNodeId.put(
                        candidate.nodeId(),
                        metrics
                );

                double reward =
                        RewardCalculator.executionReward(
                                metrics
                        );

                System.out.println(
                        "Reward = "
                                + reward
                                + " (runtime="
                                + metrics.runtimeMs()
                                + " ms, transfer="
                                + metrics.transferTimeMs()
                                + " ms)"
                );
                System.out.flush();

                policyEngine.observe(
                        candidate,
                        reward
                );
            }
        }

        if (persistModel) {
            policyEngine.save();
        }

        return new TrainingResult(
                policyEngine,
                metricsByCutNodeId
        );
    }

    /** Save bandit.json in train mode only; benchmark/workload save elsewhere. */
    private static boolean shouldPersistAfterTrain() {
        return !ResearchSettings.isBenchmarkMode()
                && !ResearchSettings.isWorkloadTrainMode();
    }
}
