package org.example.distributed;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * Drops intermediate tables on a worker (best-effort, non-fatal on error).
 *
 * <p>After Q1 cut@42 completes, cleans up {@code inter_single_n42} on worker1
 * (and worker2 if the table was replicated there).
 *
 * <p>Example:
 * <pre>
 *   dropIfExists(worker1, "inter_single_n42")
 *   → executes DROP TABLE IF EXISTS "inter_single_n42" CASCADE
 * </pre>
 */
public class IntermediateTableCleaner {

    /**
     * Idempotent drop of a named table on one worker.
     *
     * @param worker    e.g. worker1 where fragment1 materialized the cut
     * @param tableName unquoted name, e.g. inter_single_n42
     */
    public static void dropIfExists(
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
                        conn.createStatement()
        ) {
            st.execute(
                    "DROP TABLE IF EXISTS \""
                            + tableName
                            + "\" CASCADE"
            );
        }
        catch (Exception e) {
            System.out.println(
                    "Warning: could not drop "
                            + tableName
                            + " on "
                            + worker.name()
                            + ": "
                            + e.getMessage()
            );
        }
    }
}
