package org.example.sql;

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.metadata.RelColumnOrigin;
import org.apache.calcite.rel.metadata.RelMetadataQuery;

import java.util.Set;

/**
 * Builds an explicit column projection for fragment1 CTAS statements.
 *
 * <p>Calcite's {@code RelToSqlConverter} may emit {@code SELECT *} for join
 * outputs; PostgreSQL CTAS needs named columns for downstream fragment2 SQL.
 *
 * <p>Example Q1 cut@42 — output of fragment1 join:
 * <pre>
 *   SELECT
 *       customer.custkey AS custkey,
 *       orders.orderkey AS orderkey,
 *       customer.mktsegment AS mktsegment,
 *       orders.totalprice AS totalprice,
 *       …
 *   FROM customer …
 * </pre>
 */
public class DynamicProjectionBuilder {

    /**
     * Walks output columns of a RelNode and maps each to its base-table origin.
     *
     * @param node cut subtree root (fragment1), e.g. Join#42 output
     * @return SELECT list SQL (without FROM clause), one column per output field
     */
    public static String build(RelNode node) {

        RelMetadataQuery mq =
                node.getCluster()
                        .getMetadataQuery();

        StringBuilder sql =
                new StringBuilder();

        sql.append("SELECT\n");

        for (int i = 0; i < node.getRowType().getFieldCount(); i++) {

            String alias =
                    node.getRowType()
                            .getFieldNames()
                            .get(i);

            Set<RelColumnOrigin> origins =
                    mq.getColumnOrigins(
                            node,
                            i
                    );

            if (origins == null || origins.isEmpty()) {
                continue;
            }

            RelColumnOrigin origin =
                    origins.iterator()
                            .next();

            String table =
                    origin.getOriginTable()
                            .getQualifiedName()
                            .get(1)
                            .toString();

            int colIndex =
                    origin.getOriginColumnOrdinal();

            String column =
                    origin.getOriginTable()
                            .getRowType()
                            .getFieldList()
                            .get(colIndex)
                            .getName();

            sql.append("    ")
                    .append(table)
                    .append(".")
                    .append(column)
                    .append(" AS ")
                    .append(alias);

            if (i < node.getRowType().getFieldCount() - 1) {
                sql.append(",");
            }

            sql.append("\n");
        }

        return sql.toString();
    }
}
