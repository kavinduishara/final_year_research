package org.example.split;

import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.impl.AbstractTable;

public class IntermediateTable extends AbstractTable {
    private final RelDataType rowType;

    public IntermediateTable(RelDataType rowType) {
        this.rowType = rowType;
    }

    @Override
    public RelDataType getRowType(RelDataTypeFactory typeFactory) {
        return rowType;
    }
}
