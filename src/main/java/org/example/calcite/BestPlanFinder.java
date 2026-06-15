package org.example.calcite;

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelRoot;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.tools.Frameworks;
import org.apache.calcite.tools.Planner;
import org.example.sql.SqlNormalizer;

/**
 * Converts a SQL string into Calcite's logical query plan (RelNode tree).
 *
 * <p>Example input (Q1 fragment):
 * <pre>
 *   SELECT c.mktsegment, SUM(o.totalprice)
 *   FROM customer c
 *   JOIN orders o ON c.custkey = o.custkey
 *   JOIN lineitem l ON o.orderkey = l.orderkey
 *   WHERE l.shipdate > DATE '1999-05-01'
 *   GROUP BY c.mktsegment
 * </pre>
 * Returns a RelNode tree with LogicalJoin nodes (these become cut candidates).
 */
public class BestPlanFinder {

    /**
     * Parse → validate → convert SQL to the logical RelNode plan.
     *
     * @param sql  raw SQL, e.g. Q1 from ResearchQueryWorkload
     * @param ctx  Calcite context from CalcitePlannerFactory
     * @return logical RelNode root (typically LogicalAggregate over LogicalJoin chain)
     * @throws Exception if SQL is invalid or tables are missing from schema
     *
     * <p>Pipeline inside:
     * SqlNormalizer → planner.parse → planner.validate → planner.rel → root.rel
     */
    public static RelNode sqlToBestRel(String sql, CalciteContext ctx) throws Exception {
        Planner planner = Frameworks.getPlanner(ctx.frameworkConfig());

        String normalized =
                SqlNormalizer.forCalcite(sql);

        SqlNode parsed = planner.parse(normalized);
        SqlNode validated = planner.validate(parsed);
        RelRoot root = planner.rel(validated);

        return root.rel; // logical best plan
    }
}
