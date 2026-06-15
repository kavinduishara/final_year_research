package org.example.split;

import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.impl.AbstractTable;

/**
 * Calcite schema placeholder for a materialized intermediate result.
 *
 * <p>When Q1 is cut at node 42, fragment1 runs on worker1 and materializes
 * rows into PostgreSQL table {@code inter_single_n42}. Fragment2's plan
 * references this table via an {@link IntermediateTable} registered in the
 * Calcite schema so the rewriter can emit valid SQL.
 *
 * <p>Row type matches the output columns of the cut subtree (e.g. custkey,
 * orderkey, mktsegment, totalprice after the customer⋈orders⋈lineitem join).
 */
public class IntermediateTable extends AbstractTable {

    /** Column layout of the materialized intermediate relation. */
    private final RelDataType rowType;

    /**
     * @param rowType RelDataType of rows produced at the cut, e.g. 8 columns
     *                from customer+orders join output
     */
    public IntermediateTable(RelDataType rowType) {
        this.rowType = rowType;
    }

    /**
     * @param typeFactory Calcite type factory from the planning context
     * @return row type passed at construction
     */
    @Override
    public RelDataType getRowType(RelDataTypeFactory typeFactory) {
        return rowType;
    }
}
