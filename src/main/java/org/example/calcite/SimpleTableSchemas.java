package org.example.calcite;

import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.impl.AbstractTable;

/**
 * Minimal in-memory Calcite table definitions for Q1-style join examples.
 *
 * <p>Used when a full TPC-H schema is not loaded. Columns cover the joins in:
 * <pre>
 *   SELECT c.mktsegment, SUM(o.totalprice)
 *   FROM customer c
 *   JOIN orders o ON c.custkey = o.custkey
 *   JOIN lineitem l ON o.orderkey = l.orderkey
 * </pre>
 *
 * <p>Distribution assumption: worker1 hosts customer+orders, worker2 hosts lineitem.
 */
public class SimpleTableSchemas {

    /** customer(custkey, region) — join key to orders. */
    public static class CustomerTable extends AbstractTable {
        @Override
        public RelDataType getRowType(RelDataTypeFactory typeFactory) {
            return typeFactory.builder()
                    .add("custkey", typeFactory.createJavaType(Integer.class))
                    .add("region", typeFactory.createJavaType(String.class))
                    .build();
        }
    }

    /** orders(orderkey, custkey, totalprice) — bridge customer and lineitem. */
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

    /** lineitem(orderkey, shipdate) — filtered in Q1 WHERE clause. */
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
