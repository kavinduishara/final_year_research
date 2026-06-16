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



/**

 * Builds a {@link CalciteContext} by connecting to the meta PostgreSQL database.

 *

 * <p>Meta DB (application.properties):

 * <pre>

 *   meta.db.url    = jdbc:postgresql://localhost:5432/public

 *   meta.db.schema = public

 * </pre>

 * Tables visible to Calcite: customer, orders, lineitem (same schema as workers combined).

 */

public class CalcitePlannerFactory {



    /**

     * Connect to meta DB, attach every Postgres schema to Calcite, return planning context.

     *

     * @return CalciteContext with defaultSchema="public", RelBuilder ready

     * @throws SQLException if meta DB is unreachable

     * @throws IllegalStateException if meta.db.schema not found in database

     *

     * <p>Side effect: prints "=== SCHEMAS IN DATABASE ===" listing all schema names.

     */

    public static CalciteContext createFromMetaDb() throws SQLException {

        AppConfig cfg = new AppConfig("/application.properties");



        String url = cfg.get("meta.db.url");

        String user = cfg.get("meta.db.user");

        String pass = cfg.get("meta.db.pass");

        String schemaName = cfg.get("meta.db.schema"); // e.g. "public"



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



        // 2) Calcite root schema — empty container for all attached DB schemas

        SchemaPlus root = CalciteSchema.createRootSchema(false, false).plus();



        // 3) Attach ALL Postgres schemas into Calcite via JdbcSchema adapter

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



        // 4) Framework: default schema public, MySQL-style lex for parser

        FrameworkConfig frameworkConfig = Frameworks.newConfigBuilder()

                .defaultSchema(defaultSchema)

                .parserConfig(SqlParser.config().withLex(Lex.MYSQL))

                .build();



        RelBuilder relBuilder = RelBuilder.create(frameworkConfig);



        return new CalciteContext(root, defaultSchema, frameworkConfig, relBuilder);
                // each usage  : SingleCutSplitter, SchemaPrinter , BestPlanFinder , FragmentLocalityAnalyzer
    }

}


