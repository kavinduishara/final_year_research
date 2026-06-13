package org.example.distributed;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class WorkerTableProbe {

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
