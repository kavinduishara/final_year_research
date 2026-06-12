package org.example;

import org.example.benchmark.BenchmarkResult;
import org.example.benchmark.BenchmarkRunner;
import org.example.calcite.BestPlanFinder;
import org.example.calcite.CalciteContext;
import org.example.calcite.CalcitePlannerFactory;
import org.example.calcite.SchemaPrinter;
import org.example.debug.ColumnOriginPrinter;
import org.example.distributed.DistributedExecutor;
import org.example.distributed.TableDistribution;
import org.example.distributed.WorkerRegistry;
import org.example.experiment.ExperimentRunner;
import org.example.plan.CutCandidate;
import org.example.plan.CutPointCollector;
import org.example.plan.FragmentLocalityAnalyzer;
import org.example.plan.PlanStatisticsCollector;
import org.example.qos.RewardCalculator;
import org.example.rl.*;
import org.example.split.SingleCutSplitter;
import org.example.split.SplitResult;
import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.sql.dialect.PostgresqlSqlDialect;
import org.example.sql.DynamicProjectionBuilder;
import org.example.sql.FragmentSql;
import org.example.sql.FragmentSqlBuilder;
import org.example.exec.PostgresExecutor;


import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {

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

        CalciteContext ctx = CalcitePlannerFactory.createFromMetaDb();
        RelNode bestPlan = BestPlanFinder.sqlToBestRel(sql, ctx);
        SchemaPrinter.printSchema(ctx.defaultSchema());

        System.out.println("===== ORIGINAL PLAN (RelNode) =====");
        System.out.println(RelOptUtil.toString(bestPlan));

        List<RelNode> cutPoints = CutPointCollector.collectJoinCuts(bestPlan);

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

        System.out.println("\n===== CUT CANDIDATES (with locality) =====");

        for (CutCandidate c : candidates) {
            System.out.println(
                    "Node="
                            + c.nodeId()
                            + ", rows="
                            + c.estimatedRows()
                            + ", depth="
                            + c.depth()
                            + ", cost="
                            + c.estimatedCost()
            );
            System.out.println(
                    "  F1 tables="
                            + c.fragment1Tables()
                            + " -> "
                            + c.fragment1Worker()
            );
            System.out.println(
                    "  F2 tables="
                            + c.fragment2Tables()
                            + " -> "
                            + c.fragment2Worker()
            );
            System.out.println(
                    "  locality="
                            + c.localityBucket()
                            + ", executable="
                            + c.executable()
                            + ", baseTransfer="
                            + c.baseTableTransferCost()
                            + ", intermediateTransfer="
                            + c.intermediateTransferCost()
                            + ", totalTransfer="
                            + c.totalTransferCost()
            );
            System.out.println(
                    "  State="
                            + StateBuilder.from(c)
            );
        }

        QTable qTable =
                new QTable();

        for (int episode = 1;
             episode <= 100;
             episode++) {

            for (CutCandidate c : candidates) {

                if (!c.executable()) {
                    continue;
                }

                State state =
                        StateBuilder.from(c);

                String key =
                        QLearningPolicy.stateKey(
                                state,
                                c.nodeId()
                        );

                double reward =
                        RewardCalculator.reward(
                                c.totalTransferCost(),
                                c.estimatedCost()
                        );

                qTable.update(
                        key,
                        reward
                );
            }
        }
        qTable.print();

        if (cutPoints.isEmpty()) {
            System.out.println("No JOIN cut-points found. (Try a query with JOIN)");
            return;
        }

        QLearningPolicy policy =
                new QLearningPolicy(qTable);

        Action action =
                policy.choose(candidates);

        Action baselineAction =
                firstExecutableAction(
                        candidates,
                        cutPoints
                );

        System.out.println(action);
        BenchmarkResult baseline =
                BenchmarkRunner.runBaseline(
                        candidates,
                        cutPoints
                );

        BenchmarkResult rl =
                BenchmarkRunner.runQLearning(
                        candidates,
                        action
                );

        double improvement =
                ((baseline.averageTransferCost()
                        - rl.averageTransferCost())
                        /
                        baseline.averageTransferCost())
                        * 100.0;

        System.out.println(
                "\n===== POLICY COMPARISON ====="
        );

        System.out.println(
                "Baseline Transfer Cost = "
                        + baseline.averageTransferCost()
        );

        System.out.println(
                "QLearning Transfer Cost = "
                        + rl.averageTransferCost()
        );

        System.out.println(
                "Improvement = "
                        + improvement
                        + "%"
        );

        CutCandidate chosenCandidate =
                findCandidate(
                        candidates,
                        action.cutNodeId()
                );

        System.out.println(
                "\n===== RL CHOSEN CUT ====="
        );
        System.out.println(
                "Node="
                        + chosenCandidate.nodeId()
                        + ", locality="
                        + chosenCandidate.localityBucket()
                        + ", workers="
                        + chosenCandidate.fragment1Worker()
                        + " -> "
                        + chosenCandidate.fragment2Worker()
        );

        SingleCutSplitter splitter = new SingleCutSplitter();
        SplitResult split = splitter.split(bestPlan, action, ctx.rootSchema(), ctx.relBuilder());


        System.out.println("===== FRAGMENT 1 (Subtree at cut) =====");
        System.out.println(RelOptUtil.toString(split.fragment1()));

        System.out.println("\n===== FRAGMENT 1 COLUMNS =====");

        ColumnOriginPrinter.print(
                split.fragment1()
        );
        System.out.println(
                DynamicProjectionBuilder.build(
                        split.fragment1()
                )
        );

        split.fragment1()
                .getRowType()
                .getFieldList()
                .forEach(f ->
                        System.out.println(
                                f.getName()
                        )
                );

        System.out.println("===== FRAGMENT 2 (Plan with placeholder scan) =====");
        System.out.println(RelOptUtil.toString(split.fragment2()));

        System.out.println("===== PLACEHOLDER NAME =====");
        System.out.println(split.placeholderName());

        // ----- Build SQL for PostgreSQL -----
        FragmentSqlBuilder sqlBuilder = new FragmentSqlBuilder(PostgresqlSqlDialect.DEFAULT);

        System.out.println(
                RelOptUtil.toString(split.fragment1())
        );

        System.out.println("\n===== RAW FRAGMENT1 SQL =====");

        System.out.println(
                new org.example.sql.RelToSqlService(
                        PostgresqlSqlDialect.DEFAULT
                ).toSql(
                        split.fragment1()
                )
        );
        FragmentSql fragmentSql = sqlBuilder.build(split);

        System.out.println("\n===== SQL1 (CREATE TEMP TABLE ...) =====");
        System.out.println(fragmentSql.sql1());

        System.out.println("\n===== SQL2 (FINAL QUERY) =====");
        System.out.println(fragmentSql.sql2());

        DistributedExecutor executor =
                new DistributedExecutor();

        executor.execute(
                fragmentSql,
                chosenCandidate.fragment1Worker(),
                chosenCandidate.fragment2Worker()
        );

// ----- Execute separately (SQL1 then SQL2) -----
        String url = "jdbc:postgresql://localhost:5432/yourdb";
        String user = "postgres";
        String pass = "password";

//        PostgresExecutor executor = new PostgresExecutor(url, user, pass);
//        executor.execute(fragmentSql);

        System.out.println(
                WorkerRegistry.workers()
        );

        System.out.println(
                "\n===== BASELINE ACTION ====="
        );

        System.out.println(
                baselineAction
        );

        long baselineTime =
                ExperimentRunner.run(
                        bestPlan,
                        baselineAction,
                        ctx,
                        candidates
                );

        long rlTime =
                ExperimentRunner.run(
                        bestPlan,
                        action,
                        ctx,
                        candidates
                );
        double runtimeImprovement =
                ((baselineTime - rlTime)
                        / (double) baselineTime)
                        * 100.0;

        System.out.println(
                "\n===== FINAL RESEARCH RESULT ====="
        );

        System.out.println(
                "Baseline Runtime = "
                        + baselineTime
                        + " ms"
        );

        System.out.println(
                "RL Runtime = "
                        + rlTime
                        + " ms"
        );

        System.out.println(
                "Improvement = "
                        + runtimeImprovement
                        + "%"
        );
    }

    private static CutCandidate findCandidate(
            List<CutCandidate> candidates,
            int nodeId
    ) {
        return candidates.stream()
                .filter(c -> c.nodeId() == nodeId)
                .findFirst()
                .orElseThrow();
    }

    private static Action firstExecutableAction(
            List<CutCandidate> candidates,
            List<RelNode> cutPoints
    ) {
        for (RelNode node : cutPoints) {
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
}
