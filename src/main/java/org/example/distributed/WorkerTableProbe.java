package org.example.distributed;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Lightweight existence check: does a table have at least one row on a worker?
 *
 * <p>Used by {@link BaseTableShipper} to skip re-shipping lineitem to worker1
 * when a previous Q1 run already copied it.
 *
 * <p>Example:
 * <pre>
 *   hasRows(worker1, "lineitem") → false  (needs shipping from worker2)
 *   hasRows(worker1, "customer") → true   (native on worker1)
 * </pre>
 */
public class WorkerTableProbe {

    /**
     * @param worker    JDBC target, e.g. worker1
     * @param tableName unquoted table name
     * @return {@code true} if SELECT 1 … LIMIT 1 returns a row; {@code false} on empty or error
     */
    public static boolean hasRows(
            WorkerNode worker,
            String tableName
    ) {
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
                        st.executeQuery(
                                "SELECT 1 FROM \""
                                        + tableName
                                        + "\" LIMIT 1"
                        )
        ) {
            return rs.next();
        }
        catch (Exception e) {
            return false;
        }
    }
}
