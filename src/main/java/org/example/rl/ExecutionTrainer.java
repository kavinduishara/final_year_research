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

public class ExecutionTrainer {

    public record TrainingResult(
            CutPolicyEngine policyEngine,
            Map<Integer, ExecutionMetrics> metricsByCutNodeId
    ) {
    }

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

    private static boolean shouldPersistAfterTrain() {
        return !ResearchSettings.isBenchmarkMode()
                && !ResearchSettings.isWorkloadTrainMode();
    }
}
