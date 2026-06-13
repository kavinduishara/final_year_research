package org.example;

import org.example.benchmark.BenchmarkResult;
import org.example.benchmark.BenchmarkRunner;
import org.example.calcite.BestPlanFinder;
import org.example.calcite.CalciteContext;
import org.example.calcite.SchemaPrinter;
import org.example.config.ResearchSettings;
import org.example.distributed.TableDistribution;
import org.example.distributed.WorkerRegistry;
import org.example.experiment.DistributedQueryRunner;
import org.example.experiment.ExecutionSession;
import org.example.exec.ExecutionMetrics;
import org.example.plan.CutCandidate;
import org.example.plan.CutPointCollector;
import org.example.plan.FragmentLocalityAnalyzer;
import org.example.plan.PlanStatisticsCollector;
import org.example.rl.*;
import org.example.split.SplitResult;
import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.sql.dialect.PostgresqlSqlDialect;
import org.example.sql.FragmentSql;
import org.example.sql.FragmentSqlBuilder;

import java.util.List;
import java.util.Map;

public class SingleQueryRunner {

    public static void run(CalciteContext ctx) throws Exception {

        String sql = """
                SELECT c.mktsegment,
                       SUM(o.totalprice)
                FROM customer c
                JOIN orders o
                    ON c.custkey = o.custkey
                JOIN lineitem l
                    ON o.orderkey = l.orderkey
                WHERE l.shipdate > DATE '1999-05-01'
                GROUP BY c.mktsegment
    """;

        RelNode bestPlan =
                BestPlanFinder.sqlToBestRel(
                        sql,
                        ctx
                );

        SchemaPrinter.printSchema(
                ctx.defaultSchema()
        );

        System.out.println(
                "===== ORIGINAL PLAN (RelNode) ====="
        );

        System.out.println(
                RelOptUtil.toString(bestPlan)
        );

        List<RelNode> cutPoints =
                CutPointCollector.collectJoinCuts(
                        bestPlan
                );

        System.out.println(
                "\n===== CUT ORDER ====="
        );

        for (RelNode node : cutPoints) {
            System.out.println(
                    "Node = "
                            + node.getId()
            );
        }

        List<CutCandidate> candidates =
                PlanStatisticsCollector.collect(
                        bestPlan,
                        cutPoints
                );

        TableDistribution distribution =
                WorkerRegistry.tableDistribution();

        distribution.print();

        candidates =
                FragmentLocalityAnalyzer.enrichAll(
                        bestPlan,
                        candidates,
                        distribution,
                        WorkerRegistry.workers()
                );

        printCandidates(candidates);

        if (cutPoints.isEmpty()) {
            System.out.println(
                    "No JOIN cut-points found."
            );
            return;
        }

        QTable qTable;
        Map<Integer, ExecutionMetrics> metricsByCut;

        if (ResearchSettings.isInferenceMode()) {
            System.out.println(
                    "\n===== INFERENCE MODE ====="
            );

            qTable =
                    QTableStore.load(
                            ResearchSettings.qTablePath()
                    );

            qTable.print();

            metricsByCut =
                    new java.util.HashMap<>();
        }
        else {
            ExecutionTrainer.TrainingResult training =
                    ExecutionTrainer.train(
                            bestPlan,
                            ctx,
                            candidates,
                            ExecutionSession.single()
                    );

            qTable = training.qTable();
            qTable.print();

            metricsByCut =
                    training.metricsByCutNodeId();
        }

        QLearningPolicy policy =
                new QLearningPolicy(qTable);

        Action rlAction =
                policy.choose(candidates);

        if (ResearchSettings.isInferenceMode()) {
            CutCandidate chosen =
                    findCandidate(
                            candidates,
                            rlAction.cutNodeId()
                    );

            ExecutionMetrics metrics =
                    DistributedQueryRunner.execute(
                            bestPlan,
                            rlAction,
                            ctx,
                            chosen,
                            ExecutionSession.single(),
                            true
                    );

            printChosenCut(
                    chosen,
                    metrics
            );

            System.out.println(
                    "\nInference complete. Total = "
                            + metrics.totalTimeMs()
                            + " ms"
            );

            return;
        }

        DeepJoinBaselinePolicy baselinePolicy =
                new DeepJoinBaselinePolicy();

        Action baselineAction =
                baselinePolicy.choose(cutPoints);

        System.out.println(
                "\nRL action:       "
                        + rlAction
        );

        System.out.println(
                "DeepJoin action: "
                        + baselineAction
        );

        BenchmarkResult baseline =
                BenchmarkRunner.runDeepJoinBaseline(
                        candidates,
                        cutPoints,
                        metricsByCut
                );

        BenchmarkResult rl =
                BenchmarkRunner.runQLearning(
                        rlAction,
                        candidates,
                        metricsByCut
                );

        printPolicyComparison(
                baseline,
                rl
        );

        printChosenCut(
                findCandidate(
                        candidates,
                        rlAction.cutNodeId()
                ),
                metricsByCut.get(
                        rlAction.cutNodeId()
                )
        );
    }

    private static void printCandidates(
            List<CutCandidate> candidates
    ) {
        System.out.println(
                "\n===== CUT CANDIDATES ====="
        );

        for (CutCandidate c : candidates) {
            System.out.println(
                    "Node="
                            + c.nodeId()
                            + ", executable="
                            + c.executable()
                            + ", locality="
                            + c.localityBucket()
            );
        }
    }

    private static void printPolicyComparison(
            BenchmarkResult baseline,
            BenchmarkResult rl
    ) {
        System.out.println(
                "\n===== POLICY COMPARISON ====="
        );

        double improvement =
                ((baseline.totalTimeMs()
                        - rl.totalTimeMs())
                        / (double) baseline.totalTimeMs())
                        * 100.0;

        System.out.println(
                "Baseline total = "
                        + baseline.totalTimeMs()
                        + " ms"
        );

        System.out.println(
                "RL total       = "
                        + rl.totalTimeMs()
                        + " ms"
        );

        System.out.println(
                "Improvement    = "
                        + String.format(
                        "%.2f",
                        improvement
                )
                        + "%"
        );
    }

    private static void printChosenCut(
            CutCandidate chosen,
            ExecutionMetrics metrics
    ) {
        System.out.println(
                "\nRL chosen cut node = "
                        + chosen.nodeId()
        );

        if (metrics != null) {
            System.out.println(
                    "Total time = "
                            + metrics.totalTimeMs()
                            + " ms"
            );
        }
    }

    private static CutCandidate findCandidate(
            List<CutCandidate> candidates,
            int nodeId
    ) {
        return candidates.stream()
                .filter(c ->
                        c.nodeId() == nodeId
                )
                .findFirst()
                .orElseThrow();
    }
}
