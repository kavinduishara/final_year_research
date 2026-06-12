package org.example.sql;

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.metadata.RelColumnOrigin;
import org.apache.calcite.rel.metadata.RelMetadataQuery;

import java.util.Set;

public class DynamicProjectionBuilder {

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