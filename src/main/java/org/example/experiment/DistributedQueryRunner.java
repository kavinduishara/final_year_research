package org.example.experiment;

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.sql.dialect.PostgresqlSqlDialect;
import org.example.calcite.CalciteContext;
import org.example.distributed.DistributedExecutionResult;
import org.example.distributed.DistributedExecutor;
import org.example.distributed.TableDistribution;
import org.example.distributed.WorkerRegistry;
import org.example.plan.CutCandidate;
import org.example.rl.Action;
import org.example.split.SingleCutSplitter;
import org.example.split.SplitResult;
import org.example.sql.FragmentSql;
import org.example.sql.FragmentSqlBuilder;

/**
 * Bridge from plan split → SQL → distributed execution.
 *
 * <p>Called by QueryService (inference), ExecutionTrainer (training), QueryBenchmarkRunner.
 */
public class DistributedQueryRunner {

    /**
     * Split plan, generate SQL, run on workers.
     *
     * @param bestPlan full RelNode (Q1)
     * @param action   cut to try, e.g. Action(42)
     * @param ctx      Calcite context
     * @param chosen   enriched CutCandidate for worker assignment
     * @param session  e.g. ExecutionSession.single() → inter_single_n42
     * @param verbose  print worker assignment details
     * @return metrics + result rows from fragment2
     */
    public static DistributedExecutionResult execute(
            RelNode bestPlan,
            Action action,
            CalciteContext ctx,
            CutCandidate chosen,
            ExecutionSession session,
            boolean verbose
    ) throws Exception {

        if (verbose) {
            printAssignment(
                    chosen,
                    action,
                    session
            );
        }

        SingleCutSplitter splitter =
                new SingleCutSplitter();

        SplitResult split =
                splitter.split(
                        bestPlan,
                        action,
                        ctx.rootSchema(),
                        ctx.relBuilder(),
                        session
                );

        FragmentSqlBuilder builder =
                new FragmentSqlBuilder(
                        PostgresqlSqlDialect.DEFAULT
                );

        FragmentSql sql =
                builder.build(split);

        System.out.println(
                "Intermediate table = "
                        + sql.tempTableName()
        );

        TableDistribution distribution =
                WorkerRegistry.tableDistribution();

        DistributedExecutor executor =
                new DistributedExecutor();

        return executor.execute(
                sql,
                chosen,
                distribution
        );
    }

    /**
     * Split only (no DB execution). Useful for debugging generated fragments.
     * @return SplitResult with fragment1, fragment2 RelNodes
     */
    public static SplitResult splitOnly(
            RelNode bestPlan,
            Action action,
            CalciteContext ctx,
            ExecutionSession session
    ) {
        SingleCutSplitter splitter =
                new SingleCutSplitter();

        return splitter.split(
                bestPlan,
                action,
                ctx.rootSchema(),
                ctx.relBuilder(),
                session
        );
    }

    /** Prints which worker runs which fragment for this cut. */
    private static void printAssignment(
            CutCandidate chosen,
            Action action,
            ExecutionSession session
    ) {
        System.out.println(
                "\n===== RUNNING ACTION ====="
        );

        System.out.println(
                "Session = "
                        + session.id()
        );

        System.out.println(action);

        System.out.println(
                "Fragment1 tables = "
                        + chosen.fragment1Tables()
        );

        System.out.println(
                "Fragment2 tables = "
                        + chosen.fragment2Tables()
        );

        System.out.println(
                "Fragment1 worker = "
                        + chosen.fragment1Worker()
        );

        System.out.println(
                "Fragment2 worker = "
                        + chosen.fragment2Worker()
        );

        System.out.println(
                "Locality         = "
                        + chosen.localityBucket()
        );
    }
}
