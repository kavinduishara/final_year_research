package org.example.debug;

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.metadata.RelColumnOrigin;
import org.apache.calcite.rel.metadata.RelMetadataQuery;

import java.util.Set;

/**
 * Debug utility: prints where each output column of a RelNode originates.
 *
 * <p>Helpful when building {@link org.example.sql.DynamicProjectionBuilder}
 * for Q1 fragment1 CTAS — shows that {@code mktsegment} comes from
 * customer and {@code totalprice} from orders.
 *
 * <p>Example output for Join#38:
 * <pre>
 *   COLUMN: custkey
 *   [PUBLIC, CUSTOMER] columnIndex=0
 *   COLUMN: mktsegment
 *   [PUBLIC, CUSTOMER] columnIndex=6
 *   COLUMN: totalprice
 *   [PUBLIC, ORDERS] columnIndex=3
 * </pre>
 */
public class ColumnOriginPrinter {

    /**
     * Prints column name and base-table origin for every output field.
     *
     * @param node RelNode to inspect, e.g. Q1 cut subtree at Join#38 or Join#42
     */
    public static void print(RelNode node) {

        RelMetadataQuery mq =
                node.getCluster()
                        .getMetadataQuery();

        int fieldCount =
                node.getRowType()
                        .getFieldCount();

        for (int i = 0; i < fieldCount; i++) {

            String fieldName =
                    node.getRowType()
                            .getFieldNames()
                            .get(i);

            Set<RelColumnOrigin> origins =
                    mq.getColumnOrigins(
                            node,
                            i
                    );

            System.out.println(
                    "\nCOLUMN: "
                            + fieldName
            );

            if (origins == null) {
                System.out.println(
                        "No origin found"
                );
                continue;
            }

            for (RelColumnOrigin origin : origins) {

                System.out.println(
                        origin.getOriginTable()
                                .getQualifiedName()
                                + " columnIndex="
                                + origin.getOriginColumnOrdinal()
                );
            }
        }
    }
}
