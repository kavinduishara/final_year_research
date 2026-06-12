package org.example.distributed;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;

public class WorkerExecutor {

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
            for (String statement :
                    splitStatements(sql)) {
                st.execute(statement);
            }
        }

        long end =
                System.currentTimeMillis();

        long duration =
                end - start;

        System.out.println(
                "Executed on "
                        + worker.name()
                        + " in "
                        + duration
                        + " ms"
        );

        return duration;
    }

    public long executeQuery(
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

            int cols =
                    rs.getMetaData()
                            .getColumnCount();

            int rowCount = 0;

            while (rs.next() && rowCount < 10) {

                StringBuilder row =
                        new StringBuilder();

                for (int i = 1; i <= cols; i++) {

                    row.append(
                            rs.getObject(i)
                    );

                    if (i < cols) {
                        row.append(" | ");
                    }
                }

                System.out.println(row);

                rowCount++;
            }
        }

        long end =
                System.currentTimeMillis();

        long duration =
                end - start;

        System.out.println(
                "Query executed on "
                        + worker.name()
                        + " in "
                        + duration
                        + " ms"
        );

        return duration;
    }

    private static String[] splitStatements(String sql) {
        return Arrays.stream(sql.split("(?<=;)"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
    }
}
