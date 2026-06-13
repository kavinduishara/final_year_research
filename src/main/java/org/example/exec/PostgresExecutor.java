package org.example.exec;

import org.example.sql.FragmentSql;

import java.sql.*;

public class PostgresExecutor {

    private final String jdbcUrl;
    private final String user;
    private final String pass;

    public PostgresExecutor(String jdbcUrl,
                            String user,
                            String pass) {
        this.jdbcUrl = jdbcUrl;
        this.user = user;
        this.pass = pass;
    }

    public ExecutionMetrics execute(FragmentSql sql)
            throws SQLException {

        long start = System.nanoTime();

        try (Connection conn =
                     DriverManager.getConnection(
                             jdbcUrl,
                             user,
                             pass)) {

            conn.setAutoCommit(true);

            System.out.println(
                    "\n===== EXEC SQL1 (fragment1 -> TEMP TABLE) ====="
            );
            System.out.println(sql.sql1());

            try (Statement st =
                         conn.createStatement()) {

                st.execute(sql.sql1());
            }

            System.out.println(
                    "\n===== EXEC SQL2 (final query) ====="
            );
            System.out.println(sql.sql2());

            try (Statement st =
                         conn.createStatement();
                 ResultSet rs =
                         st.executeQuery(sql.sql2())) {

                printFirstRows(rs, 10);
            }
        }

        long end = System.nanoTime();

        long elapsedMs =
                (end - start) / 1_000_000;

        System.out.println(
                "\n===== EXECUTION METRICS ====="
        );

        System.out.println(
                "Execution Time = "
                        + elapsedMs
                        + " ms"
        );

        return new ExecutionMetrics(
                elapsedMs,
                0,
                0,
                0
        );
    }

    private void printFirstRows(ResultSet rs,
                                int limit)
            throws SQLException {

        ResultSetMetaData md =
                rs.getMetaData();

        int cols =
                md.getColumnCount();

        int count = 0;

        while (rs.next() && count < limit) {

            StringBuilder sb =
                    new StringBuilder();

            sb.append("Row ")
                    .append(count + 1)
                    .append(": ");

            for (int c = 1;
                 c <= cols;
                 c++) {

                sb.append(md.getColumnLabel(c))
                        .append("=")
                        .append(rs.getObject(c));

                if (c < cols) {
                    sb.append(", ");
                }
            }

            System.out.println(sb);

            count++;
        }

        if (count == 0) {
            System.out.println(
                    "(No rows returned)"
            );
        }
    }
}