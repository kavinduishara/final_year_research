package org.example.distributed;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class IntermediateTableCleaner {

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
