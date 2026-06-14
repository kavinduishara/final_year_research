package org.example.experiment;

import org.apache.calcite.rel.RelNode;
import org.example.calcite.BestPlanFinder;
import org.example.calcite.CalciteContext;
import org.example.config.ResearchSettings;
import org.example.distributed.ResearchEnvironmentCleaner;
import org.example.distributed.TableDistribution;
import org.example.distributed.WorkerRegistry;
import org.example.plan.CutCandidate;
import org.example.plan.CutPointCollector;
import org.example.plan.FragmentLocalityAnalyzer;
import org.example.plan.PlanStatisticsCollector;
import org.example.rl.CutPolicyEngine;
import org.example.rl.ExecutionTrainer;

import java.util.ArrayList;
import java.util.List;

public final class WorkloadTrainer {

    public record WorkloadTrainRow(
            int queryNumber,
            String queryName,
            int executableCuts,
            int modelSizeAfter
    ) {
    }

    private WorkloadTrainer() {
    }

    public static List<WorkloadTrainRow> run(
            CalciteContext ctx
    ) throws Exception {

        TableDistribution distribution =
                WorkerRegistry.tableDistribution();

        List<ResearchQueryWorkload.ResearchQuery> queries =
                ResearchQueryWorkload.all();

        CutPolicyEngine policyEngine =
                CutPolicyEngine.loadForTraining(
                        ResearchSettings.resumeQTable()
                );

        List<WorkloadTrainRow> rows =
                new ArrayList<>();

        System.out.println(
                "\n===== MULTI-QUERY TRAINING ====="
        );

        System.out.println(
                "Queries            = "
                        + queries.size()
        );

        System.out.println(
                "Algorithm          = "
                        + policyEngine.algorithmLabel()
        );

        System.out.println(
                "Episodes per query = "
                        + ResearchSettings.trainingEpisodes()
        );

        System.out.println(
                "Resume existing    = "
                        + ResearchSettings.resumeQTable()
        );

        System.out.println(
                "Starting model size = "
                        + modelSize(policyEngine)
        );

        System.out.flush();

        for (int i = 0; i < queries.size(); i++) {

            ResearchQueryWorkload.ResearchQuery query =
                    queries.get(i);

            int queryNumber = i + 1;

            System.out.println(
                    "\n"
                            + "=".repeat(60)
            );

            System.out.println(
                    "TRAIN QUERY "
                            + queryNumber
                            + " / "
                            + queries.size()
                            + ": "
                            + query.name()
            );

            System.out.println(
                    "=".repeat(60)
            );

            System.out.println(query.sql());
            System.out.flush();

            ResearchEnvironmentCleaner.cleanupBetweenQueries();

            RelNode plan =
                    BestPlanFinder.sqlToBestRel(
                            query.sql(),
                            ctx
                    );

            List<RelNode> cutPoints =
                    CutPointCollector.collectJoinCuts(
                            plan
                    );

            if (cutPoints.isEmpty()) {
                System.out.println(
                        "Skipped: no JOIN cut points."
                );
                continue;
            }

            List<CutCandidate> candidates =
                    PlanStatisticsCollector.collect(
                            plan,
                            cutPoints
                    );

            candidates =
                    FragmentLocalityAnalyzer.enrichAll(
                            plan,
                            candidates,
                            distribution,
                            WorkerRegistry.workers()
                    );

            long executable =
                    candidates.stream()
                            .filter(CutCandidate::executable)
                            .count();

            System.out.println(
                    "Executable cuts = "
                            + executable
                            + " / "
                            + candidates.size()
            );

            if (executable == 0) {
                System.out.println(
                        "Skipped: no executable cuts."
                );
                continue;
            }

            ExecutionSession session =
                    ExecutionSession.forQuery(
                            queryNumber
                    );

            ExecutionTrainer.train(
                    plan,
                    ctx,
                    candidates,
                    session,
                    policyEngine,
                    false
            );

            WorkloadTrainRow row =
                    new WorkloadTrainRow(
                            queryNumber,
                            query.name(),
                            (int) executable,
                            modelSize(policyEngine)
                    );

            rows.add(row);

            System.out.println(
                    "Model size after query "
                            + queryNumber
                            + " = "
                            + row.modelSizeAfter()
            );
            System.out.flush();
        }

        policyEngine.save();

        printSummary(rows, policyEngine);

        System.out.println(
                "\nMulti-query training complete. "
                        + "Set training.mode=inference to run queries "
                        + "using the trained model."
        );

        return rows;
    }

    private static int modelSize(
            CutPolicyEngine policyEngine
    ) {
        if (policyEngine.bandit() != null) {
            return policyEngine.bandit().observations();
        }

        return policyEngine.qTable().entries().size();
    }

    private static void printSummary(
            List<WorkloadTrainRow> rows,
            CutPolicyEngine policyEngine
    ) {
        System.out.println(
                "\n===== WORKLOAD TRAINING SUMMARY ====="
        );

        System.out.printf(
                "%-4s %-28s %-8s %-10s%n",
                "#",
                "Query",
                "Cuts",
                "ModelSize"
        );

        for (WorkloadTrainRow row : rows) {
            System.out.printf(
                    "%-4d %-28s %-8d %-10d%n",
                    row.queryNumber(),
                    row.queryName(),
                    row.executableCuts(),
                    row.modelSizeAfter()
            );
        }

        System.out.println(
                "\nFinal model size = "
                        + modelSize(policyEngine)
        );

        policyEngine.print();
    }
}
