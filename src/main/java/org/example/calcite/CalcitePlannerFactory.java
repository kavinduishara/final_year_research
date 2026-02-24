package org.example.calcite;

import org.apache.calcite.adapter.jdbc.JdbcSchema;
import org.apache.calcite.config.Lex;
import org.apache.calcite.jdbc.CalciteSchema;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.tools.FrameworkConfig;
import org.apache.calcite.tools.Frameworks;
import org.apache.calcite.tools.RelBuilder;
import org.example.config.AppConfig;
import org.postgresql.ds.PGSimpleDataSource;

import java.sql.SQLException;

public class CalcitePlannerFactory {

    public static CalciteContext createFromMetaDb() throws SQLException {
        AppConfig cfg = new AppConfig("/application.properties");

        String url = cfg.get("meta.db.url");
        String user = cfg.get("meta.db.user");
        String pass = cfg.get("meta.db.pass");
        String schemaName = cfg.get("meta.db.schema"); // your default schema

        // Postgres DataSource (Meta DB)
        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setURL(url);
        ds.setUser(user);
        ds.setPassword(pass);

        // 1) Print all schemas in this database (debug)
        try (var conn = ds.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT schema_name FROM information_schema.schemata ORDER BY schema_name")) {

            System.out.println("=== SCHEMAS IN DATABASE ===");
            while (rs.next()) {
                System.out.println("Schema: " + rs.getString(1));
            }
        }

        // 2) Calcite root schema
        SchemaPlus root = CalciteSchema.createRootSchema(false, false).plus();

        // 3) Attach ALL Postgres schemas into Calcite
        SchemaPlus defaultSchema = null;

        try (var conn = ds.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT schema_name FROM information_schema.schemata ORDER BY schema_name")) {

            while (rs.next()) {
                String dbSchema = rs.getString("schema_name");

                SchemaPlus attached = root.add(
                        dbSchema,
                        JdbcSchema.create(root, dbSchema, ds, null, dbSchema)
                );

                if (dbSchema.equalsIgnoreCase(schemaName)) {
                    defaultSchema = attached;
                }
            }
        }

        if (defaultSchema == null) {
            throw new IllegalStateException(
                    "Default schema '" + schemaName + "' not found in database. " +
                            "Check meta.db.schema in application.properties"
            );
        }

        // Optional: print what Calcite sees (schemas + tables)
        System.out.println("\n=== CALCITE SCHEMAS + TABLES ===");
        for (String sName : root.getSubSchemaNames()) {
            SchemaPlus s = root.getSubSchema(sName);
            System.out.println("Calcite schema: " + sName + " tables=" + s.getTableNames());
        }

        // 4) Build framework config using the chosen default schema
        FrameworkConfig frameworkConfig = Frameworks.newConfigBuilder()
                .defaultSchema(defaultSchema)
                .parserConfig(SqlParser.config().withLex(Lex.MYSQL))
                .build();

        RelBuilder relBuilder = RelBuilder.create(frameworkConfig);

        // Return context: root + default schema
        return new CalciteContext(root, defaultSchema, frameworkConfig, relBuilder);
    }
}