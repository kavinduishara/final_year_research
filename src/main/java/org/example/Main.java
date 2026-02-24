package org.example;

import org.example.calcite.BestPlanFinder;
import org.example.calcite.CalciteContext;
import org.example.calcite.CalcitePlannerFactory;
import org.example.calcite.SchemaPrinter;
import org.example.plan.CutPointCollector;
import org.example.rl.Action;
import org.example.rl.DummyRandomPolicy;
import org.example.rl.Policy;
import org.example.split.SingleCutSplitter;
import org.example.split.SplitResult;
import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.sql.dialect.PostgresqlSqlDialect;
import org.example.sql.FragmentSql;
import org.example.sql.FragmentSqlBuilder;
import org.example.exec.PostgresExecutor;


import java.util.List;
import java.util.Properties;

public class Main {
    public static void main(String[] args) throws Exception {

        String sql = """
            SELECT c.region, SUM(o.totalprice)
            FROM customer c
            JOIN orders o ON c.custkey = o.custkey
            JOIN lineitem l ON o.orderkey = l.orderkey
            WHERE l.shipdate > '1996-01-01'
            GROUP BY c.region
            """;

        CalciteContext ctx = CalcitePlannerFactory.createFromMetaDb();
        RelNode bestPlan = BestPlanFinder.sqlToBestRel(sql, ctx);
        SchemaPrinter.printSchema(ctx.defaultSchema());

        System.out.println("===== ORIGINAL PLAN (RelNode) =====");
        System.out.println(RelOptUtil.toString(bestPlan));

        List<RelNode> cutPoints = CutPointCollector.collectJoinCuts(bestPlan);
        if (cutPoints.isEmpty()) {
            System.out.println("No JOIN cut-points found. (Try a query with JOIN)");
            return;
        }

        Policy policy = new DummyRandomPolicy();
        Action action = policy.choose(cutPoints);

        System.out.println("===== DUMMY RL ACTION =====");
        System.out.println(action);

        SingleCutSplitter splitter = new SingleCutSplitter();
        SplitResult split = splitter.split(bestPlan, action, ctx.rootSchema(), ctx.relBuilder());


        System.out.println("===== FRAGMENT 1 (Subtree at cut) =====");
        System.out.println(RelOptUtil.toString(split.fragment1()));

        System.out.println("===== FRAGMENT 2 (Plan with placeholder scan) =====");
        System.out.println(RelOptUtil.toString(split.fragment2()));

        System.out.println("===== PLACEHOLDER NAME =====");
        System.out.println(split.placeholderName());

        // ----- Build SQL for PostgreSQL -----
        FragmentSqlBuilder sqlBuilder = new FragmentSqlBuilder(PostgresqlSqlDialect.DEFAULT);
        FragmentSql fragmentSql = sqlBuilder.build(split);

        System.out.println("\n===== SQL1 (CREATE TEMP TABLE ...) =====");
        System.out.println(fragmentSql.sql1());

        System.out.println("\n===== SQL2 (FINAL QUERY) =====");
        System.out.println(fragmentSql.sql2());

// ----- Execute separately (SQL1 then SQL2) -----
//        String url = "jdbc:postgresql://localhost:5432/yourdb";
//        String user = "postgres";
//        String pass = "password";

//        PostgresExecutor executor = new PostgresExecutor(url, user, pass);
//        executor.execute(fragmentSql);


    }
}
