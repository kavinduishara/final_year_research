package org.example.query;

import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.rel.RelNode;
import org.example.benchmark.BenchmarkResult;
import org.example.benchmark.BenchmarkRunner;
import org.example.calcite.BestPlanFinder;
import org.example.calcite.CalciteContext;
import org.example.calcite.SchemaPrinter;
import org.example.config.ResearchSettings;
import org.example.distributed.DistributedExecutionResult;
import org.example.distributed.ResearchEnvironmentCleaner;
import org.example.distributed.TableDistribution;
import org.example.distributed.WorkerRegistry;
import org.example.experiment.DistributedQueryRunner;
import org.example.experiment.ExecutionSession;
import org.example.exec.ExecutionMetrics;
import org.example.plan.CutCandidate;
import org.example.plan.CutPointCollector;
import org.example.plan.FragmentLocalityAnalyzer;
import org.example.plan.PlanStatisticsCollector;
import org.example.rl.Action;
import org.example.rl.BaselinePolicyFactory;
import org.example.rl.CutSelection;
import org.example.rl.CutPolicyEngine;
import org.example.rl.CutSelection;
import org.example.rl.ExecutionTrainer;

import java.util.List;
import java.util.Map;

public final class QueryService {

    public record PreparedQuery(
            RelNode bestPlan,
            List<RelNode> cutPoints,
            List<CutCandidate> candidates
    ) {
    }

    private QueryService() {
    }

    public static QueryResult run(
            CalciteContext ctx,
            String sql,
            boolean verbose
    ) throws Exception {

        PreparedQuery prepared =
                prepare(
                        ctx,
                        sql,
                        verbose
                );

        ResearchEnvironmentCleaner.cleanupBetweenQueries();

        if (prepared.cutPoints().isEmpty()) {
            throw new IllegalStateException(
                    "No JOIN cut-points found for query"
            );
        }

        if (ResearchSettings.isInferenceMode()) {
            return runInference(
                    ctx,
                    sql,
                    prepared,
                    verbose
            );
        }

        return runTrain(
                ctx,
                sql,
                prepared,
                verbose
        );
    }

    public static PreparedQuery prepare(
            CalciteContext ctx,
            String sql,
            boolean verbose
    ) throws Exception {

        RelNode bestPlan =
                BestPlanFinder.sqlToBestRel(
                        sql,
                        ctx
                );

        if (verbose) {

            SchemaPrinter.printSchema(
                    ctx.defaultSchema()
            );

            System.out.println(
                    "===== ORIGINAL PLAN (RelNode) ====="
            );

            System.out.println(
                    RelOptUtil.toString(bestPlan)
            );
        }

        List<RelNode> cutPoints =
                CutPointCollector.collectJoinCuts(
                        bestPlan
                );

        if (verbose) {

            System.out.println(
                    "\n===== CUT ORDER ====="
            );

            for (RelNode node : cutPoints) {
                System.out.println(
                        "Node = "
                                + node.getId()
                );
            }
        }

        List<CutCandidate> candidates =
                PlanStatisticsCollector.collect(
                        bestPlan,
                        cutPoints
                );

        TableDistribution distribution =
                WorkerRegistry.tableDistribution();

        if (verbose) {
            distribution.print();
        }

        candidates =
                FragmentLocalityAnalyzer.enrichAll(
                        bestPlan,
                        candidates,
                        distribution,
                        WorkerRegistry.workers()
                );

        if (verbose) {
            printCandidates(candidates);
        }

        return new PreparedQuery(
                bestPlan,
                cutPoints,
                candidates
        );
    }

    private static QueryResult runInference(
            CalciteContext ctx,
            String sql,
            PreparedQuery prepared,
            boolean verbose
    ) throws Exception {

        System.out.println(
                "\n===== INFERENCE MODE ====="
        );

        CutPolicyEngine policyEngine =
                CutPolicyEngine.loadForInference();

        if (verbose) {
            policyEngine.print();
        }

        CutSelection selection =
                policyEngine.choose(
                        prepared.candidates()
                );

        printSelection(selection);

        DistributedExecutionResult execution =
                DistributedQueryRunner.execute(
                        prepared.bestPlan(),
                        selection.action(),
                        ctx,
                        selection.candidate(),
                        ExecutionSession.single(),
                        verbose
                );

        QueryResult result =
                toQueryResult(
                        sql,
                        selection,
                        execution
                );

        result.printSummary();
        result.printResults();

        return result;
    }

    private static QueryResult runTrain(
            CalciteContext ctx,
            String sql,
            PreparedQuery prepared,
            boolean verbose
    ) throws Exception {

        System.out.println(
                "\n===== TRAIN MODE ====="
        );

        ExecutionTrainer.TrainingResult training =
                ExecutionTrainer.train(
                        prepared.bestPlan(),
                        ctx,
                        prepared.candidates(),
                        ExecutionSession.single()
                );

        CutPolicyEngine policyEngine =
                training.policyEngine();

        if (verbose) {
            policyEngine.print();
        }

        CutSelection selection =
                policyEngine.choose(
                        prepared.candidates(),
                        training.metricsByCutNodeId()
                );

        printSelection(selection);

        Action baselineAction =
                BaselinePolicyFactory.choose(
                        prepared.candidates(),
                        prepared.cutPoints()
                );

        Map<Integer, ExecutionMetrics> metricsByCut =
                training.metricsByCutNodeId();

        BenchmarkResult baseline =
                BenchmarkRunner.runBaseline(
                        prepared.candidates(),
                        prepared.cutPoints(),
                        metricsByCut
                );

        BenchmarkResult rl =
                BenchmarkRunner.runQLearning(
                        selection.action(),
                        prepared.candidates(),
                        metricsByCut
                );

        printPolicyComparison(
                baseline,
                rl
        );

        ExecutionMetrics rlMetrics =
                metricsByCut.get(
                        selection.action().cutNodeId()
                );

        QueryResult result =
                new QueryResult(
                        sql,
                        selection.candidate().nodeId(),
                        selection.reason(),
                        selection.bestQ(),
                        List.of(),
                        List.of(),
                        rlMetrics
                );

        result.printSummary();

        System.out.println(
                "\nModel saved ("
                        + policyEngine.algorithmLabel()
                        + "). Use training.mode=inference "
                        + "for fast queries with results."
        );

        return result;
    }

    private static QueryResult toQueryResult(
            String sql,
            CutSelection selection,
            DistributedExecutionResult execution
    ) {
        return new QueryResult(
                sql,
                selection.candidate().nodeId(),
                selection.reason(),
                selection.bestQ(),
                execution.columnNames(),
                execution.rows(),
                execution.metrics()
        );
    }

    private static void printSelection(
            CutSelection selection
    ) {
        System.out.println(
                "\n===== CUT SELECTION ====="
        );

        System.out.println(
                "Cut node  = "
                        + selection.candidate().nodeId()
        );

        System.out.println(
                "Reason    = "
                        + selection.reason()
        );

        System.out.println(
                "Locality  = "
                        + selection.candidate().localityBucket()
        );

        if (selection.reason()
                == CutSelection.SelectionReason.RL
                || selection.reason()
                == CutSelection.SelectionReason.BANDIT_LINUCB
                || selection.reason()
                == CutSelection.SelectionReason.BANDIT_THOMPSON
                || selection.reason()
                == CutSelection.SelectionReason.EXECUTION_BEST) {
            System.out.println(
                    "Best score = "
                            + selection.bestQ()
            );
        }
    }

    private static void printCandidates(
            List<CutCandidate> candidates
    ) {
        System.out.println(
                "\n===== CUT CANDIDATES ====="
        );

        for (CutCandidate candidate : candidates) {
            System.out.println(
                    "Node="
                            + candidate.nodeId()
                            + ", executable="
                            + candidate.executable()
                            + ", locality="
                            + candidate.localityBucket()
                            + ", transfer="
                            + candidate.totalTransferCost()
                            + ", baselineEst="
                            + candidate.baselineDecisionCost()
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
                baseline.totalTimeMs() == 0
                        ? 0
                        : ((baseline.totalTimeMs()
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
}
