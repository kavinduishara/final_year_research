package org.example.plan;

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelVisitor;
import org.apache.calcite.rel.core.TableScan;

import java.util.LinkedHashSet;
import java.util.Set;

public class RelTableCollector {

    public static Set<String> collect(RelNode root) {
        Set<String> tables = new LinkedHashSet<>();

        new RelVisitor() {
            @Override
            public void visit(RelNode node, int ordinal, RelNode parent) {
                if (node instanceof TableScan scan) {
                    tables.add(
                            tableName(scan)
                    );
                }
                super.visit(node, ordinal, parent);
            }
        }.go(root);

        return tables;
    }

    private static String tableName(TableScan scan) {
        var names =
                scan.getTable()
                        .getQualifiedName();

        return names
                .get(names.size() - 1)
                .toLowerCase();
    }
}
