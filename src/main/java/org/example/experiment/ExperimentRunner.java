package org.example.experiment;

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.sql.dialect.PostgresqlSqlDialect;
import org.example.calcite.CalciteContext;
import org.example.distributed.DistributedExecutor;
import org.example.plan.CutCandidate;
import org.example.rl.Action;
import org.example.split.SingleCutSplitter;
import org.example.split.SplitResult;
import org.example.sql.FragmentSql;
import org.example.sql.FragmentSqlBuilder;

import java.util.List;

public class ExperimentRunner {

    public static long run(
            RelNode bestPlan,
            Action action,
            CalciteContext ctx,
            List<CutCandidate> candidates
    ) throws Exception {

        CutCandidate chosen =
                candidates.stream()
                        .filter(c ->
                                c.nodeId()
                                        == action.cutNodeId()
                        )
                        .findFirst()
                        .orElseThrow();

        System.out.println(
                "\n===== RUNNING ACTION ====="
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

        System.out.println(
                "Transfer cost    = "
                        + chosen.totalTransferCost()
        );

        SingleCutSplitter splitter =
                new SingleCutSplitter();

        SplitResult split =
                splitter.split(
                        bestPlan,
                        action,
                        ctx.rootSchema(),
                        ctx.relBuilder()
                );

        FragmentSqlBuilder builder =
                new FragmentSqlBuilder(
                        PostgresqlSqlDialect.DEFAULT
                );

        FragmentSql sql =
                builder.build(split);

        DistributedExecutor executor =
                new DistributedExecutor();

        return executor.execute(
                sql,
                chosen.fragment1Worker(),
                chosen.fragment2Worker()
        );
    }
}
