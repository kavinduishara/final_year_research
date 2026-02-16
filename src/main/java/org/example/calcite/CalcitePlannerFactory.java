package org.example.calcite;

import org.apache.calcite.config.Lex;
import org.apache.calcite.jdbc.CalciteSchema;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.tools.FrameworkConfig;
import org.apache.calcite.tools.Frameworks;
import org.apache.calcite.tools.Planner;
import org.apache.calcite.tools.RelBuilder;

public class CalcitePlannerFactory {

    public static CalciteContext createWithInMemorySchema() {
        SchemaPlus root = CalciteSchema.createRootSchema(false, false).plus();

        root.add("customer", new SimpleTableSchemas.CustomerTable());
        root.add("orders", new SimpleTableSchemas.OrdersTable());
        root.add("lineitem", new SimpleTableSchemas.LineitemTable());

        FrameworkConfig config = Frameworks.newConfigBuilder()
                .defaultSchema(root)
                .parserConfig(SqlParser.config().withLex(Lex.MYSQL))
                .build();

        Planner planner = Frameworks.getPlanner(config);
        RelBuilder relBuilder = RelBuilder.create(config);

        return new CalciteContext(root, planner, config, relBuilder);
    }
}
