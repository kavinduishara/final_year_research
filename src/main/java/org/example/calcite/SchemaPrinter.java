package org.example.calcite;

import org.apache.calcite.jdbc.JavaTypeFactoryImpl;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.Table;

/**
 * Debug utility: prints tables and columns visible to Calcite's schema.
 *
 * <p>After registering TPC-H tables for Q1 planning, output looks like:
 * <pre>
 *   ===== CALCITE VISIBLE SCHEMA: PUBLIC =====
 *   Table: customer
 *     - custkey : INTEGER
 *     - mktsegment : VARCHAR
 *   Table: orders
 *     - orderkey : INTEGER
 *     - custkey : INTEGER
 *     - totalprice : DOUBLE
 *   Table: lineitem
 *     - orderkey : INTEGER
 *     - shipdate : VARCHAR
 * </pre>
 */
public class SchemaPrinter {

    /**
     * Prints every table name and column type in the given schema.
     *
     * @param schema root schema from {@link CalciteContext}, e.g. PUBLIC
     */
    public static void printSchema(SchemaPlus schema) {
        System.out.println("\n===== CALCITE VISIBLE SCHEMA: " + schema.getName() + " =====");

        JavaTypeFactoryImpl typeFactory = new JavaTypeFactoryImpl();

        for (String tableName : schema.getTableNames()) {
            Table table = schema.getTable(tableName);
            if (table == null) continue;

            RelDataType rowType = table.getRowType(typeFactory);

            System.out.println("\nTable: " + tableName);
            for (RelDataTypeField f : rowType.getFieldList()) {
                System.out.println("  - " + f.getName() + " : " + f.getType());
            }
        }
    }
}
