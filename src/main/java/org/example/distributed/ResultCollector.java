package org.example.distributed;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class ResultCollector {

    private ResultCollector() {
    }

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
