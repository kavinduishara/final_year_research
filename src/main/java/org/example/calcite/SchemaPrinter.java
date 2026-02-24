package org.example.calcite;

import org.apache.calcite.jdbc.JavaTypeFactoryImpl;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.Table;

import java.util.Map;

public class SchemaPrinter {

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