package org.example.experiment;

import org.apache.calcite.rel.RelNode;
import org.example.benchmark.BenchmarkResult;
import org.example.benchmark.BenchmarkRunner;
import org.example.calcite.BestPlanFinder;
import org.example.calcite.CalciteContext;
import org.example.config.ResearchSettings;
import org.example.distributed.TableDistribution;
import org.example.distributed.WorkerRegistry;
import org.example.distributed.ResearchEnvironmentCleaner;
import org.example.plan.CutCandidate;
import org.example.plan.CutPointCollector;
import org.example.plan.FragmentLocalityAnalyzer;
import org.example.plan.PlanStatisticsCollector;
import org.example.rl.Action;
import org.example.rl.BaselinePolicyFactory;
import org.example.rl.CutPolicyEngine;
import org.example.rl.CutSelection;
import org.example.rl.ExecutionTrainer;

import java.util.ArrayList;
import java.util.List;

public class QueryBenchmarkRunner {

    public record QueryBenchmarkRow(
            int queryNumber,
            String queryName,
            int baselineCutNode,
            int learnedCutNode,
            long baselineTotalMs,
            long learnedTotalMs,
            double improvementPercent,
            boolean sameCut
    ) {
    }

    public static List<QueryBenchmarkRow> run(
            CalciteContext ctx
    ) throws Exception {

        TableDistribution distribution =
                WorkerRegistry.tableDistribution();

        List<ResearchQueryWorkload.ResearchQuery> queries =
                ResearchQueryWorkload.all();

        List<QueryBenchmarkRow> rows =
                new ArrayList<>();

        System.out.println(
                "\n===== 10-QUERY RESEARCH BENCHMARK ====="
        );

        System.out.println(
                "Episodes per query = "
                        + ResearchSettings.trainingEpisodes()
        );

        System.out.println(
                "Shipping enabled   = "
                        + ResearchSettings.shippingEnabled()
        );

        System.out.println(
                "Baseline policy    = "
                        + BaselinePolicyFactory.displayName()
                        + " ("
                        + ResearchSettings.baselineMode()
                        + ")"
        );

        System.out.flush();

        CutPolicyEngine sharedPolicy =
                CutPolicyEngine.createFresh();

        System.out.println(
                "Learned policy     = "
                        + sharedPolicy.algorithmLabel()
        );

        for (int i = 0; i < queries.size(); i++) {

            ResearchQueryWorkload.ResearchQuery query =
                    queries.get(i);

            int queryNumber = i + 1;

            System.out.println(
                    "\n"
                            + "=".repeat(60)
            );

            System.out.println(
                    "QUERY "
                            + queryNumber
                            + " / 10: "
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

            ExecutionTrainer.TrainingResult training =
                    ExecutionTrainer.train(
                            plan,
                            ctx,
                            candidates,
                            session,
                            sharedPolicy,
                            false
                    );

            CutSelection learnedSelection =
                    sharedPolicy.choose(
                            candidates,
                            training.metricsByCutNodeId()
                    );

            Action learnedAction =
                    learnedSelection.action();

            Action baselineAction =
                    BaselinePolicyFactory.choose(
                            candidates,
                            cutPoints
                    );

            BenchmarkResult baseline =
                    BenchmarkRunner.runBaseline(
                            candidates,
                            cutPoints,
                            training.metricsByCutNodeId()
                    );

            BenchmarkResult learned =
                    BenchmarkRunner.runLearnedPolicy(
                            learnedAction,
                            candidates,
                            training.metricsByCutNodeId()
                    );

            double improvement =
                    baseline.totalTimeMs() == 0
                            ? 0
                            : ((baseline.totalTimeMs()
                            - learned.totalTimeMs())
                            / (double) baseline.totalTimeMs())
                            * 100.0;

            boolean sameCut =
                    baselineAction.cutNodeId()
                            == learnedAction.cutNodeId();

            QueryBenchmarkRow row =
                    new QueryBenchmarkRow(
                            queryNumber,
                            query.name(),
                            baselineAction.cutNodeId(),
                            learnedAction.cutNodeId(),
                            baseline.totalTimeMs(),
                            learned.totalTimeMs(),
                            improvement,
                            sameCut
                    );

            rows.add(row);

            printQueryResult(
                    row,
                    baseline,
                    learned,
                    candidates,
                    baselineAction,
                    learnedAction,
                    learnedSelection.reason()
            );
        }

        sharedPolicy.save();

        printSummaryTable(rows);

        return rows;
    }

    private static void printQueryResult(
            QueryBenchmarkRow row,
            BenchmarkResult baseline,
            BenchmarkResult rl,
            List<CutCandidate> candidates,
            Action baselineAction,
            Action learnedAction,
            CutSelection.SelectionReason learnedReason
    ) {
        System.out.println(
                "\n----- Query "
                        + row.queryNumber()
                        + " result -----"
        );

        System.out.println(
                BaselinePolicyFactory.displayName()
                        + " cut node = "
                        + row.baselineCutNode()
                        + ", total = "
                        + row.baselineTotalMs()
                        + " ms"
        );

        System.out.println(
                "LinUCB cut node   = "
                        + row.learnedCutNode()
                        + ", total = "
                        + row.learnedTotalMs()
                        + " ms"
        );

        System.out.println(
                "Improvement       = "
                        + String.format(
                        "%.2f",
                        row.improvementPercent()
                )
                        + "%"
        );

        System.out.println(
                "Learned reason    = "
                        + learnedReason
        );

        if (row.sameCut()) {
            System.out.println(
                    "Note: same cut chosen (0% expected)."
            );
        }
        else if (row.improvementPercent() > 0) {
            printCutDecisionHint(
                    candidates,
                    baselineAction,
                    learnedAction
            );
        }
        else if (row.improvementPercent() < 0) {
            System.out.println(
                    "Note: baseline faster on this query "
                            + "(RL did not beat static estimate)."
            );
        }
    }

    private static void printCutDecisionHint(
            List<CutCandidate> candidates,
            Action baselineAction,
            Action learnedAction
    ) {
        CutCandidate baselineCut =
                findCandidate(
                        candidates,
                        baselineAction.cutNodeId()
                );

        CutCandidate rlCut =
                findCandidate(
                        candidates,
                        learnedAction.cutNodeId()
                );

        System.out.println(
                "Note: baseline est. cost = "
                        + (long) baselineCut.baselineDecisionCost()
                        + ", learned cut est. cost = "
                        + (long) rlCut.baselineDecisionCost()
                        + " — LinUCB chose better after execution."
        );
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

    private static void printSummaryTable(
            List<QueryBenchmarkRow> rows
    ) {
        System.out.println(
                "\n===== BENCHMARK SUMMARY (ALL QUERIES) ====="
        );

        System.out.printf(
                "%-4s %-28s %-8s %-8s %-12s %-12s %-10s%n",
                "#",
                "Query",
                "BaseCut",
                "LearnedCut",
                "BaseMs",
                "LearnedMs",
                "Improve%"
        );

        double totalBaseline = 0;
        double totalLearned = 0;

        for (QueryBenchmarkRow row : rows) {
            System.out.printf(
                    "%-4d %-28s %-8d %-8d %-12d %-12d %-10.2f%n",
                    row.queryNumber(),
                    row.queryName(),
                    row.baselineCutNode(),
                    row.learnedCutNode(),
                    row.baselineTotalMs(),
                    row.learnedTotalMs(),
                    row.improvementPercent()
            );

            totalBaseline += row.baselineTotalMs();
            totalLearned += row.learnedTotalMs();
        }

        double overall =
                totalBaseline == 0
                        ? 0
                        : ((totalBaseline - totalLearned)
                        / totalBaseline)
                        * 100.0;

        System.out.println(
                "\nOverall improvement = "
                        + String.format(
                        "%.2f",
                        overall
                )
                        + "%"
        );

        System.out.println(
                "Total baseline time = "
                        + (long) totalBaseline
                        + " ms"
        );

        System.out.println(
                "Total LinUCB time   = "
                        + (long) totalLearned
                        + " ms"
        );
    }
}
