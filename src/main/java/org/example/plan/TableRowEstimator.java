package org.example.plan;

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelVisitor;
import org.apache.calcite.rel.core.TableScan;
import org.apache.calcite.rel.metadata.RelMetadataQuery;

import java.util.HashMap;
import java.util.Map;

/**
 * Estimates row counts per base table in a logical plan.
 *
 * <p>Uses Calcite metadata when trustworthy; falls back to TPC-H scale defaults.
 *
 * <p>Example Q1 plan:
 * <pre>
 *   estimate(root) → {
 *     "customer"  : 150_000,
 *     "orders"    : 1_500_000,
 *     "lineitem"  : 6_000_000
 *   }
 * </pre>
 * These counts feed transfer-cost estimates in {@link FragmentLocalityAnalyzer}.
 */
public class TableRowEstimator {

    /** TPC-H SF1 approximate cardinalities used when metadata is missing. */
    private static final Map<String, Double> TPCH_DEFAULT_ROWS =
            Map.of(
                    "customer", 150_000.0,
                    "orders", 1_500_000.0,
                    "lineitem", 6_000_000.0
            );

    /** Metadata row counts below this are treated as unreliable. */
    private static final double MIN_TRUSTED_ROWS = 1_000.0;

    /**
     * Walks all {@link TableScan} nodes and collects row counts by table name.
     *
     * @param root best plan root for Q1 (or any query)
     * @return map tableName → rows, e.g. {"orders" → 1_500_000.0}
     */
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

    /**
     * Extracts the unqualified table name from a scan node.
     *
     * @param scan e.g. Scan: PUBLIC.CUSTOMER
     * @return lowercase name, e.g. "customer"
     */
    private static String tableName(TableScan scan) {
        var names =
                scan.getTable()
                        .getQualifiedName();

        return names
                .get(names.size() - 1)
                .toLowerCase();
    }

    /**
     * Returns metadata count when ≥ {@link #MIN_TRUSTED_ROWS}, else TPC-H default.
     *
     * @param table         e.g. "lineitem"
     * @param metadataRows  Calcite estimate or null
     * @return trusted row count, e.g. 6_000_000.0
     */
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
