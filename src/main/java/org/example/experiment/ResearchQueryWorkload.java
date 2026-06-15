package org.example.experiment;

import java.util.List;

/**
 * TPC-H-inspired research query workload (10 queries, all customer⋈orders⋈lineitem).
 *
 * <p>Q1 is the canonical teaching example:
 * <pre>
 *   SELECT c.mktsegment, SUM(o.totalprice)
 *   FROM customer c
 *   JOIN orders o ON c.custkey = o.custkey
 *   JOIN lineitem l ON o.orderkey = l.orderkey
 *   WHERE l.shipdate > DATE '1999-05-01'
 *   GROUP BY c.mktsegment
 * </pre>
 *
 * <p>With worker1={customer,orders} and worker2={lineitem}, each query yields
 * cut candidates at JOIN nodes (typically ids 38 and 42 in the logical plan).
 */
public final class ResearchQueryWorkload {

    /**
     * One named SQL query in the benchmark suite.
     *
     * @param name short label, e.g. "Q1_mktsegment_revenue"
     * @param sql  full SQL text (may include trailing semicolon; normalized later)
     */
    public record ResearchQuery(
            String name,
            String sql
    ) {
    }

    private ResearchQueryWorkload() {
    }

    /**
     * @return all 10 research queries [Q1 … Q10]
     */
    public static List<ResearchQuery> all() {
        return List.of(
                q1(),
                q2(),
                q3(),
                q4(),
                q5(),
                q6(),
                q7(),
                q8(),
                q9(),
                q10()
        );
    }

    /** @return Q1 SQL (default query for demos and Main). */
    public static String defaultSql() {
        return q1().sql();
    }

    private static ResearchQuery q1() {
        return new ResearchQuery(
                "Q1_mktsegment_revenue",
                """
                        SELECT c.mktsegment,
                               SUM(o.totalprice)
                        FROM customer c
                        JOIN orders o ON c.custkey = o.custkey
                        JOIN lineitem l ON o.orderkey = l.orderkey
                        WHERE l.shipdate > DATE '1999-05-01'
                        GROUP BY c.mktsegment
                        """
        );
    }

    private static ResearchQuery q2() {
        return new ResearchQuery(
                "Q2_selective_segment",
                """
                        SELECT c.mktsegment,
                               COUNT(DISTINCT o.orderkey)
                        FROM customer c
                        JOIN orders o ON c.custkey = o.custkey
                        JOIN lineitem l ON o.orderkey = l.orderkey
                        WHERE c.mktsegment = 'BUILDING'
                          AND o.orderdate >= DATE '1995-01-01'
                        GROUP BY c.mktsegment
                        """
        );
    }

    private static ResearchQuery q3() {
        return new ResearchQuery(
                "Q3_quantity_by_shipmode",
                """
                        SELECT l.shipmode,
                               SUM(l.quantity)
                        FROM customer c
                        JOIN orders o ON c.custkey = o.custkey
                        JOIN lineitem l ON o.orderkey = l.orderkey
                        WHERE l.discount < 0.05
                        GROUP BY l.shipmode
                        """
        );
    }

    private static ResearchQuery q4() {
        return new ResearchQuery(
                "Q4_avg_price_by_priority",
                """
                        SELECT o.orderpriority,
                               AVG(o.totalprice)
                        FROM customer c
                        JOIN orders o ON c.custkey = o.custkey
                        JOIN lineitem l ON o.orderkey = l.orderkey
                        WHERE l.shipdate BETWEEN DATE '1995-01-01' AND DATE '1998-12-31'
                        GROUP BY o.orderpriority
                        """
        );
    }

    private static ResearchQuery q5() {
        return new ResearchQuery(
                "Q5_lineitems_per_segment",
                """
                        SELECT c.mktsegment,
                               COUNT(*)
                        FROM customer c
                        JOIN orders o ON c.custkey = o.custkey
                        JOIN lineitem l ON o.orderkey = l.orderkey
                        GROUP BY c.mktsegment
                        """
        );
    }

    private static ResearchQuery q6() {
        return new ResearchQuery(
                "Q6_extended_price_sum",
                """
                        SELECT c.mktsegment,
                               SUM(l.extendedprice)
                        FROM customer c
                        JOIN orders o ON c.custkey = o.custkey
                        JOIN lineitem l ON o.orderkey = l.orderkey
                        WHERE l.returnflag = 'N'
                        GROUP BY c.mktsegment
                        """
        );
    }

    private static ResearchQuery q7() {
        return new ResearchQuery(
                "Q7_selective_shipdate",
                """
                        SELECT l.shipmode,
                               SUM(l.quantity)
                        FROM customer c
                        JOIN orders o ON c.custkey = o.custkey
                        JOIN lineitem l ON o.orderkey = l.orderkey
                        WHERE l.shipdate = DATE '1998-09-27'
                        GROUP BY l.shipmode
                        """
        );
    }

    private static ResearchQuery q8() {
        return new ResearchQuery(
                "Q8_automobile_high_qty",
                """
                        SELECT c.mktsegment,
                               AVG(l.extendedprice)
                        FROM customer c
                        JOIN orders o ON c.custkey = o.custkey
                        JOIN lineitem l ON o.orderkey = l.orderkey
                        WHERE c.mktsegment = 'AUTOMOBILE'
                          AND l.quantity > 45
                        GROUP BY c.mktsegment
                        """
        );
    }

    private static ResearchQuery q9() {
        return new ResearchQuery(
                "Q9_tax_sum_by_segment",
                """
                        SELECT c.mktsegment,
                               SUM(l.tax)
                        FROM customer c
                        JOIN orders o ON c.custkey = o.custkey
                        JOIN lineitem l ON o.orderkey = l.orderkey
                        WHERE l.linestatus = 'F'
                        GROUP BY c.mktsegment
                        """
        );
    }

    private static ResearchQuery q10() {
        return new ResearchQuery(
                "Q10_revenue_filtered",
                """
                        SELECT c.mktsegment,
                               SUM(l.extendedprice * (1 - l.discount))
                        FROM customer c
                        JOIN orders o ON c.custkey = o.custkey
                        JOIN lineitem l ON o.orderkey = l.orderkey
                        WHERE l.shipdate > DATE '1996-01-01'
                          AND o.orderstatus = 'F'
                        GROUP BY c.mktsegment
                        """
        );
    }
}
