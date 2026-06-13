package org.example.experiment;

import java.util.List;

public final class ResearchQueryWorkload {

    public record ResearchQuery(
            String name,
            String sql
    ) {
    }

    private ResearchQueryWorkload() {
    }

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
                "Q2_order_count_by_segment",
                """
                        SELECT c.mktsegment,
                               COUNT(DISTINCT o.orderkey)
                        FROM customer c
                        JOIN orders o ON c.custkey = o.custkey
                        JOIN lineitem l ON o.orderkey = l.orderkey
                        WHERE o.orderdate >= DATE '1995-01-01'
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
                "Q7_discount_by_returnflag",
                """
                        SELECT l.returnflag,
                               AVG(l.discount)
                        FROM customer c
                        JOIN orders o ON c.custkey = o.custkey
                        JOIN lineitem l ON o.orderkey = l.orderkey
                        WHERE o.totalprice > 1000
                        GROUP BY l.returnflag
                        """
        );
    }

    private static ResearchQuery q8() {
        return new ResearchQuery(
                "Q8_max_order_by_nation",
                """
                        SELECT c.nationkey,
                               MAX(o.totalprice)
                        FROM customer c
                        JOIN orders o ON c.custkey = o.custkey
                        JOIN lineitem l ON o.orderkey = l.orderkey
                        GROUP BY c.nationkey
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
