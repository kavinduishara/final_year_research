package org.example.distributed;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Drains a JDBC {@link ResultSet} into a {@link CollectedRows} structure.
 *
 * <p>Used after fragment2 runs on worker2 for Q1:
 * <pre>
 *   collect(rs) → CollectedRows(
 *     ["mktsegment", "sum"],
 *     [["BUILDING", 123456789.0], …]
 *   )
 * </pre>
 */
public final class ResultCollector {

    private ResultCollector() {
    }

    /**
     * Reads all rows from the result set into memory.
     *
     * @param rs open result set from {@link WorkerExecutor#executeQueryCollect}
     * @return column names and row values
     * @throws SQLException on JDBC read failure
     */
    public static CollectedRows collect(
            ResultSet rs
    ) throws SQLException {

        ResultSetMetaData meta =
                rs.getMetaData();

        int columnCount =
                meta.getColumnCount();

        List<String> columnNames =
                new ArrayList<>(columnCount);

        for (int i = 1; i <= columnCount; i++) {
            columnNames.add(
                    meta.getColumnLabel(i)
            );
        }

        List<List<Object>> rows =
                new ArrayList<>();

        while (rs.next()) {

            List<Object> row =
                    new ArrayList<>(columnCount);

            for (int i = 1; i <= columnCount; i++) {
                row.add(
                        rs.getObject(i)
                );
            }

            rows.add(row);
        }

        return new CollectedRows(
                columnNames,
                rows
        );
    }
}
