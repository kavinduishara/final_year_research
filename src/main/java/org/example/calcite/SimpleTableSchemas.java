package org.example.calcite;

import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.impl.AbstractTable;

public class SimpleTableSchemas {

    // Minimal columns needed for common JOIN examples
    public static class CustomerTable extends AbstractTable {
        @Override
        public RelDataType getRowType(RelDataTypeFactory typeFactory) {
            return typeFactory.builder()
                    .add("custkey", typeFactory.createJavaType(Integer.class))
                    .add("region", typeFactory.createJavaType(String.class))
                    .build();
        }
    }

    public static class OrdersTable extends AbstractTable {
        @Override
        public RelDataType getRowType(RelDataTypeFactory typeFactory) {
            return typeFactory.builder()
                    .add("orderkey", typeFactory.createJavaType(Integer.class))
                    .add("custkey", typeFactory.createJavaType(Integer.class))
                    .add("totalprice", typeFactory.createJavaType(Double.class))
                    .build();
        }
    }

    public static class LineitemTable extends AbstractTable {
        @Override
        public RelDataType getRowType(RelDataTypeFactory typeFactory) {
            return typeFactory.builder()
                    .add("orderkey", typeFactory.createJavaType(Integer.class))
                    .add("shipdate", typeFactory.createJavaType(String.class))
                    .build();
        }
    }
}
