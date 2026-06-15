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

/**
 * Multi-query training loop: updates one shared LinUCB model across Q1–Q10.
 *
 * <p>Unlike {@link QueryBenchmarkRunner}, does not compare to baseline — only
 * trains the bandit by executing every executable cut per query.
 *
 * <p>Q1 training flow:
 * <pre>
 *   plan Q1 → cuts [38, 42] → enrich(worker1, worker2)
 *   → ExecutionTrainer.train(session=q1, episodes=N)
 *   → model observes rewards for inter_q1_n38 and inter_q1_n42
 * </pre>
 */
public final class WorkloadTrainer {

    /**
     * Summary row after training one query.
     *
     * @param queryNumber      1-based index
     * @param queryName        e.g. Q1_mktsegment_revenue
     * @param executableCuts   count of runnable cuts for that query
     * @param modelSizeAfter   bandit observation count after this query
     */
    public record WorkloadTrainRow(
            int queryNumber,
            String queryName,
            int executableCuts,
            int modelSizeAfter
    ) {
    }

    private WorkloadTrainer() {
    }

    /**
     * Trains LinUCB on all workload queries sequentially; saves model at end.
     *
     * @param ctx Calcite planning context
     * @return training summary rows (skipped queries omitted)
     * @throws Exception on failure
     */
    public static List<WorkloadTrainRow> run(
            CalciteContext ctx
    ) throws Exception {

        TableDistribution distribution =
                WorkerRegistry.tableDistribution();

        List<ResearchQueryWorkload.ResearchQuery> queries =
                ResearchQueryWorkload.all();

        CutPolicyEngine policyEngine =
                CutPolicyEngine.loadForTraining(
                        ResearchSettings.resumeBanditModel()
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
                        + ResearchSettings.resumeBanditModel()
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
                //     Calcite builds a logical plan tree, roughly:

                // LogicalAggregate (GROUP BY mktsegment)
                // └── LogicalJoin  ← CUT #2 (orders ⋈ lineitem)
                //         ├── LogicalJoin  ← CUT #1 (customer ⋈ orders)
                //         │     ├── Scan: customer
                //         │     └── Scan: orders
                //         └── Scan: lineitem

            List<RelNode> cutPoints =
                    CutPointCollector.collectJoinCuts(
                            plan
                    );
//     Find legal cut points,Rule: only LogicalJoin nodes can be cut.


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
        return policyEngine.bandit().observations();
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
