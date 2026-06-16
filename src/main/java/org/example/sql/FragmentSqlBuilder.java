package org.example.sql;



import org.apache.calcite.sql.SqlDialect;

import org.example.split.SplitResult;



/**

 * Converts a {@link SplitResult} into PostgreSQL scripts for two-worker execution.

 *

 * <p>Q1 cut@42 example (worker1 runs sql1, worker2 runs sql2):

 * <pre>

 *   sql1 = DROP + CREATE TABLE "inter_single_n42" AS

 *          SELECT customer.custkey AS custkey, … FROM customer JOIN orders JOIN lineitem …

 *   sql2 = SELECT c.mktsegment, SUM(…) FROM "inter_single_n42" … GROUP BY …

 * </pre>
 
 */

public class FragmentSqlBuilder {



    private final RelToSqlService toSql;



    /**

     * @param dialect PostgreSQL dialect from {@link PgSqlDialectFactory#postgres()}

     */

    public FragmentSqlBuilder(SqlDialect dialect) {

        this.toSql = new RelToSqlService(dialect);

    }



    /**

     * Builds drop/create/final SQL for a split plan.

     *

     * @param split fragment1, fragment2, and placeholder from {@link org.example.split.SingleCutSplitter}

     * @return {@link FragmentSql} ready for {@link org.example.distributed.WorkerExecutor}

     */

    public FragmentSql build(SplitResult split) {

        String placeholder = split.placeholderName();

        String quoted =

                quoteIdentifier(placeholder);



        String rawSql =

                toSql.toSql(

                        split.fragment1()

                );



        String projection =

                DynamicProjectionBuilder.build(

                        split.fragment1()

                );



        int fromPos =

                rawSql.indexOf("FROM");



        String frag1Select =

                projection

                        + "\n"

                        + rawSql.substring(fromPos);



        String dropSql =

                "DROP TABLE IF EXISTS "

                        + quoted

                        + " CASCADE";



        String createSql =

                "CREATE TABLE "

                        + quoted

                        + " AS\n"

                        + frag1Select;



        String sql2 =

                toSql.toSql(split.fragment2()) + ";";



        return new FragmentSql(

                dropSql,

                createSql,

                sql2,

                placeholder

        );

    }



    /**

     * @param name unquoted identifier, e.g. inter_single_n42

     * @return PostgreSQL-quoted name, e.g. "inter_single_n42"

     */

    private static String quoteIdentifier(String name) {

        return "\"" + name + "\"";

    }

}

