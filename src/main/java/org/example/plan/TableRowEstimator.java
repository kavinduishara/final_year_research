package org.example.plan;

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelVisitor;
import org.apache.calcite.rel.core.TableScan;
import org.apache.calcite.rel.metadata.RelMetadataQuery;

import java.util.HashMap;
import java.util.Map;

public class TableRowEstimator {

    private static final Map<String, Double> TPCH_DEFAULT_ROWS =
            Map.of(
                    "customer", 150_000.0,
                    "orders", 1_500_000.0,
                    "lineitem", 6_000_000.0
            );

    private static final double MIN_TRUSTED_ROWS = 1_000.0;

    public static Map<String, Double> estimate(RelNode root) {
        Map<String, Double> rows =
                new HashMap<>();

        RelMetadataQuery mq =
                root.getCluster()
                        .getMetadataQuery();

        new RelVisitor() {
            @Override
            public void visit(
                    RelNode node,
                    int ordinal,
                    RelNode parent
            ) {
                if (node instanceof TableScan scan) {
                    String name =
                            tableName(scan);

                    Double count =
                            mq.getRowCount(node);

                    rows.put(
                            name,
                            trustedRowCount(
                                    name,
                                    count
                            )
                    );
                }

                super.visit(
                        node,
                        ordinal,
                        parent
                );
            }
        }.go(root);

        return rows;
    }

    private static String tableName(TableScan scan) {
        var names =
                scan.getTable()
                        .getQualifiedName();

        return names
                .get(names.size() - 1)
                .toLowerCase();
    }

    private static double trustedRowCount(
            String table,
            Double metadataRows
    ) {
        double fallback =
                TPCH_DEFAULT_ROWS.getOrDefault(
                        table,
                        10_000.0
                );

        if (metadataRows == null
                || metadataRows < MIN_TRUSTED_ROWS) {
            return fallback;
        }

        return metadataRows;
    }
}
