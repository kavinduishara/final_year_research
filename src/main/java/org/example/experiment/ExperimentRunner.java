package org.example.experiment;

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.sql.dialect.PostgresqlSqlDialect;
import org.example.calcite.CalciteContext;
import org.example.distributed.DistributedExecutor;
import org.example.rl.Action;
import org.example.split.SingleCutSplitter;
import org.example.split.SplitResult;
import org.example.sql.FragmentSql;
import org.example.sql.FragmentSqlBuilder;

public class ExperimentRunner {

    public static long run(
            RelNode bestPlan,
            Action action,
            CalciteContext ctx
    ) throws Exception {

        System.out.println(
                "\n===== RUNNING ACTION ====="
        );

        System.out.println(action);

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

        return executor.execute(sql);
    }
}