package org.example.plan;

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelVisitor;
import org.apache.calcite.rel.core.TableScan;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Collects base-table names referenced in a RelNode subtree.
 *
 * <p>Example Q1 — subtree at cut node 38 (customer ⋈ orders):
 * <pre>
 *   collect(cutNode@38) → {customer, orders}
 *   collect(root)       → {customer, orders, lineitem}
 * </pre>
 * Used by {@link FragmentLocalityAnalyzer} to split tables between fragments.
 */
public class RelTableCollector {

    /**
     * Depth-first walk; every {@link TableScan} adds its table name.
     *
     * @param root subtree or full plan
     * @return ordered set of lowercase table names, e.g. {customer, orders}
     */
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

    /**
     * @param scan Calcite table scan node
     * @return unqualified lowercase name, e.g. "lineitem"
     */
    private static String tableName(TableScan scan) {
        var names =
                scan.getTable()
                        .getQualifiedName();

        return names
                .get(names.size() - 1)
                .toLowerCase();
    }
}
