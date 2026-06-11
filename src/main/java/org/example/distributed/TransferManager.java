package org.example.distributed;

import java.sql.*;

public class TransferManager {

    public long transfer(
            WorkerNode source,
            WorkerNode target,
            String tableName
    ) throws Exception {

        long start =
                System.currentTimeMillis();

        try (
                Connection src =
                        DriverManager.getConnection(
                                source.jdbcUrl(),
                                source.user(),
                                source.password()
                        );

                Connection dst =
                        DriverManager.getConnection(
                                target.jdbcUrl(),
                                target.user(),
                                target.password()
                        )
        ) {

            dst.setAutoCommit(false);

            Statement srcStmt =
                    src.createStatement();

            ResultSet rs =
                    srcStmt.executeQuery(
                            "SELECT * FROM " + tableName
                    );

            ResultSetMetaData md =
                    rs.getMetaData();

            int cols =
                    md.getColumnCount();

            StringBuilder create =
                    new StringBuilder();

            create.append(
                    "CREATE TABLE IF NOT EXISTS \""
                            + tableName
                            + "\" ("
            );

            for (int i = 1; i <= cols; i++) {

                String columnName =
                        md.getColumnName(i);

                String columnType =
                        md.getColumnTypeName(i);

                System.out.println(
                        columnName
                                + " -> "
                                + columnType
                );

                create.append(columnName)
                        .append(" ")
                        .append(columnType);

                if (i < cols) {
                    create.append(",");
                }
            }

            create.append(")");

            dst.createStatement()
                    .execute(
                            "DROP TABLE IF EXISTS \""
                                    + tableName
                                    + "\""
                    );

            dst.createStatement()
                    .execute(
                            create.toString()
                    );

            System.out.println(
                    "\nCREATE SQL:"
            );

            System.out.println(
                    create
            );

            StringBuilder insertSql =
                    new StringBuilder();

            insertSql.append(
                    "INSERT INTO \""
                            + tableName
                            + "\" VALUES ("
            );

            for (int i = 1; i <= cols; i++) {

                insertSql.append("?");

                if (i < cols) {
                    insertSql.append(",");
                }
            }

            insertSql.append(")");

            PreparedStatement ps =
                    dst.prepareStatement(
                            insertSql.toString()
                    );

            int rowCount = 0;

            while (rs.next()) {

                for (int i = 1; i <= cols; i++) {

                    ps.setObject(
                            i,
                            rs.getObject(i)
                    );
                }

                ps.addBatch();

                rowCount++;

                if (rowCount % 1000 == 0) {
                    ps.executeBatch();
                }
            }

            ps.executeBatch();

            dst.commit();

            System.out.println(
                    "Rows copied = "
                            + rowCount
            );

            System.out.println(
                    "Transferred "
                            + tableName
                            + " to "
                            + target.name()
            );

            long end =
                    System.currentTimeMillis();

            long duration =
                    end - start;

            System.out.println(
                    "Transfer Time = "
                            + duration
                            + " ms"
            );

            return duration;
        }
    }
}