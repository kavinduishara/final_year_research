package org.example.distributed;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

public class WorkerExecutor {

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

    public record QueryExecutionResult(
            CollectedRows data,
            long executionTimeMs
    ) {
    }

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
