package org.example.distributed;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

/**
 * JDBC executor for SQL on a single PostgreSQL worker.
 *
 * <p>Runs the two-phase Q1 cut workflow:
 * <ol>
 *   <li>worker1: {@code DROP + CREATE TABLE "inter_single_n42" AS …} (fragment1)</li>
 *   <li>worker2: final SELECT over intermediate (fragment2)</li>
 * </ol>
 *
 * <p>Example timing output: {@code Created intermediate on worker1 in 842 ms}
 */
public class WorkerExecutor {

    /**
     * Executes a DROP statement (idempotent cleanup).
     *
     * @param worker  e.g. worker1 hosting customer+orders
     * @param dropSql e.g. {@code DROP TABLE IF EXISTS "inter_single_n42" CASCADE}
     * @return elapsed milliseconds
     */
    public long executeDrop(
            WorkerNode worker,
            String dropSql
    ) throws Exception {

        long start =
                System.currentTimeMillis();

        try (
                Connection conn =
                        DriverManager.getConnection(
                                worker.jdbcUrl(),
                                worker.user(),
                                worker.password()
                        );

                Statement st =
                        conn.createStatement()
        ) {
            st.execute(dropSql);
        }

        return System.currentTimeMillis() - start;
    }

    /**
     * Materializes fragment1 into an intermediate table (CTAS).
     *
     * @param worker    worker assigned to fragment1, e.g. worker1 for cut@38
     * @param createSql CREATE TABLE "inter_single_n42" AS SELECT …
     * @return elapsed milliseconds
     */
    public long executeCreate(
            WorkerNode worker,
            String createSql
    ) throws Exception {

        long start =
                System.currentTimeMillis();

        try (
                Connection conn =
                        DriverManager.getConnection(
                                worker.jdbcUrl(),
                                worker.user(),
                                worker.password()
                        );

                Statement st =
                        conn.createStatement()
        ) {
            st.execute(createSql);
        }

        long duration =
                System.currentTimeMillis() - start;

        System.out.println(
                "Created intermediate on "
                        + worker.name()
                        + " in "
                        + duration
                        + " ms"
        );

        return duration;
    }

    /**
     * Runs an arbitrary update/DDL statement and logs duration.
     *
     * @param worker target worker node
     * @param sql    statement to execute
     * @return elapsed milliseconds
     */
    public long executeUpdate(
            WorkerNode worker,
            String sql
    ) throws Exception {

        long start =
                System.currentTimeMillis();

        try (
                Connection conn =
                        DriverManager.getConnection(
                                worker.jdbcUrl(),
                                worker.user(),
                                worker.password()
                        );

                Statement st =
                        conn.createStatement()
        ) {
            st.execute(sql);
        }

        long duration =
                System.currentTimeMillis() - start;

        System.out.println(
                "Executed on "
                        + worker.name()
                        + " in "
                        + duration
                        + " ms"
        );

        return duration;
    }

    /**
     * Query result plus measured execution time.
     *
     * @param data            collected rows and column names
     * @param executionTimeMs wall-clock query time on the worker
     */
    public record QueryExecutionResult(
            CollectedRows data,
            long executionTimeMs
    ) {
    }

    /**
     * Executes a SELECT and materializes all rows in memory.
     *
     * @param worker e.g. worker2 for Q1 fragment2
     * @param sql    final query, e.g. aggregate over "inter_single_n42"
     * @return rows and timing, e.g. 5 rows in 120 ms
     */
    public QueryExecutionResult executeQueryCollect(
            WorkerNode worker,
            String sql
    ) throws Exception {

        long start =
                System.currentTimeMillis();

        try (
                Connection conn =
                        DriverManager.getConnection(
                                worker.jdbcUrl(),
                                worker.user(),
                                worker.password()
                        );

                Statement st =
                        conn.createStatement();

                ResultSet rs =
                        st.executeQuery(sql)
        ) {
            CollectedRows data =
                    ResultCollector.collect(rs);

            long duration =
                    System.currentTimeMillis() - start;

            System.out.println(
                    "Query executed on "
                            + worker.name()
                            + " in "
                            + duration
                            + " ms ("
                            + data.rows().size()
                            + " rows)"
            );

            return new QueryExecutionResult(
                    data,
                    duration
            );
        }
    }

    /**
     * Runs a query, prints up to 10 preview rows, returns elapsed ms.
     *
     * @param worker target worker
     * @param sql    SELECT statement
     * @return execution time in milliseconds
     */
    public long executeQuery(
            WorkerNode worker,
            String sql
    ) throws Exception {

        QueryExecutionResult result =
                executeQueryCollect(
                        worker,
                        sql
                );

        printPreview(
                result.data()
        );

        return result.executionTimeMs();
    }

    /** Prints first 10 result rows to stdout for debugging. */
    private static void printPreview(
            CollectedRows data
    ) {
        int limit =
                Math.min(
                        10,
                        data.rows().size()
                );

        for (int r = 0; r < limit; r++) {

            List<Object> row =
                    data.rows().get(r);

            StringBuilder line =
                    new StringBuilder();

            for (int c = 0; c < row.size(); c++) {

                line.append(row.get(c));

                if (c < row.size() - 1) {
                    line.append(" | ");
                }
            }

            System.out.println(line);
        }

        if (data.rows().size() > limit) {
            System.out.println(
                    "... ("
                            + (data.rows().size() - limit)
                            + " more rows)"
            );
        }
    }
}
