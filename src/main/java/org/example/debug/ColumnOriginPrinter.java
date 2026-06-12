package org.example.debug;

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.metadata.RelColumnOrigin;
import org.apache.calcite.rel.metadata.RelMetadataQuery;

import java.util.Set;

public class ColumnOriginPrinter {

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