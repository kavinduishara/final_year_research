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
import org.example.rl.ExecutionTrainer;

import java.util.List;
import java.util.Map;

/**
 * Core orchestrator: plan SQL → find cuts → train or infer → execute distributed query.
 *
 * <p>Example (Q1, training.mode=train):
 * <pre>
 *   run(ctx, Q1_sql, verbose=true)
 *   → prepare() finds cuts [38, 42]
 *   → runTrain() executes each cut, updates bandit, compares vs weighted-ship baseline
 *   → returns QueryResult(chosenCut=42, rows=[], metrics=ExecutionMetrics(...))
 * </pre>
 */
public final class QueryService {

    /**
     * Output of the planning phase (before train/inference).
     *
     * @param bestPlan   Calcite RelNode root for the SQL
     * @param cutPoints  raw JOIN RelNodes (e.g. [Join#38, Join#42])
     * @param candidates enriched CutCandidates with worker/transfer info
     */
    public record PreparedQuery(
            RelNode bestPlan,
            List<RelNode> cutPoints,
            List<CutCandidate> candidates
    ) {
    }

    private QueryService() {
    }

    /**
     * Main entry for single-query modes (train or inference).
     *
     * @param ctx     Calcite context from factory
     * @param sql     e.g. Q1: "SELECT c.mktsegment, SUM(o.totalprice) FROM customer c JOIN ..."
     * @param verbose print plan, cuts, and candidate details
     * @return QueryResult with chosen cut, metrics, and rows (rows only in inference)
     */
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

    /**
     * Planning only: SQL → RelNode → cut points → enriched candidates.
     * Used by QueryService, QueryBenchmarkRunner, WorkloadTrainer.
     *
     * @return PreparedQuery ready for training or cut selection
     */
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

    /**
     * Inference: load bandit.json → pick best cut → execute once → return full rows.
     */
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

    /**
     * Train: execute every executable cut (cheap first), update bandit, compare vs baseline.
     * Does not re-run final query — uses cached metrics from training executions.
     */
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

        BenchmarkResult learned =
                BenchmarkRunner.runLearnedPolicy(
                        selection.action(),
                        prepared.candidates(),
                        metricsByCut
                );

        printPolicyComparison(
                baseline,
                learned
        );

        ExecutionMetrics learnedMetrics =
                metricsByCut.get(
                        selection.action().cutNodeId()
                );

        QueryResult result =
                new QueryResult(
                        sql,
                        selection.candidate().nodeId(),
                        selection.reason(),
                        selection.bestScore(),
                        List.of(),
                        List.of(),
                        learnedMetrics
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

    /** Builds QueryResult from distributed execution output. */
    private static QueryResult toQueryResult(
            String sql,
            CutSelection selection,
            DistributedExecutionResult execution
    ) {
        return new QueryResult(
                sql,
                selection.candidate().nodeId(),
                selection.reason(),
                selection.bestScore(),
                execution.columnNames(),
                execution.rows(),
                execution.metrics()
        );
    }

    /** Prints chosen cut node, reason, locality, and score. */
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
                == CutSelection.SelectionReason.LINUCB
                || selection.reason()
                == CutSelection.SelectionReason.EXECUTION_BEST) {
            System.out.println(
                    "Best score = "
                            + selection.bestScore()
            );
        }
    }

    /** Debug: print all cut candidates with transfer and baseline estimates. */
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

    /** Compare baseline vs LinUCB total time after training. */
    private static void printPolicyComparison(
            BenchmarkResult baseline,
            BenchmarkResult learned
    ) {
        System.out.println(
                "\n===== POLICY COMPARISON ====="
        );

        double improvement =
                baseline.totalTimeMs() == 0
                        ? 0
                        : ((baseline.totalTimeMs()
                        - learned.totalTimeMs())
                        / (double) baseline.totalTimeMs())
                        * 100.0;

        System.out.println(
                "Baseline total = "
                        + baseline.totalTimeMs()
                        + " ms"
        );

        System.out.println(
                "LinUCB total   = "
                        + learned.totalTimeMs()
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
