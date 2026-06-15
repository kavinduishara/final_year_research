package org.example.sql;

/**
 * Post-processes generated SQL to replace wildcard projections with explicit columns.
 *
 * <p>Research/debug helper for Q1-style joins where {@code SELECT *} from an
 * intermediate table breaks column disambiguation in PostgreSQL.
 *
 * <p>Example input (fragment2 over {@code inter_single_n42}):
 * <pre>
 *   SELECT * FROM "inter_single_n42" …
 * </pre>
 * Output expands {@code SELECT *} to named customer/orders columns (see implementation).
 */
public class IntermediateSqlFixer {

    /**
     * Replaces the first {@code SELECT *} occurrence with an explicit column list.
     *
     * @param sql generated fragment SQL, possibly containing SELECT *
     * @return SQL with expanded projection for customer+orders columns
     */
    public static String fix(String sql) {

        return sql.replace(
                "SELECT *",
                """
                        SELECT
                            customer.custkey AS custkey,
                            customer.name AS name,
                            customer.address AS address,
                            customer.nationkey AS nationkey,
                            customer.phone AS phone,
                            customer.acctbal AS acctbal,
                            customer.mktsegment AS mktsegment,
                            customer.comment AS comment,
                        
                            orders.orderkey AS orderkey,
                            orders.custkey AS custkey0,
                            orders.orderstatus AS orderstatus,
                            orders.totalprice AS totalprice,
                            orders.orderdate AS orderdate,
                            orders.orderpriority AS orderpriority,
                            orders.clerk AS clerk,
                            orders.shippriority AS shippriority,
                            orders.comment AS comment0
                """
        );
    }
}
