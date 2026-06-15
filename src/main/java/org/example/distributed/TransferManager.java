package org.example.distributed;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

/**
 * Copies a PostgreSQL table row-by-row from one worker to another.
 *
 * <p>Used when Q1 requires shipping a base table (e.g. lineitem from worker2
 * to worker1 for a shallow cut) or replicating an intermediate result.
 *
 * <p>Example:
 * <pre>
 *   transfer(worker2, worker1, "lineitem")
 *   → SELECT * FROM "lineitem" on worker2
 *   → CREATE + INSERT batches on worker1
 *   → returns elapsed ms (e.g. 45000 ms for 6M rows)
 * </pre>
 */
public class TransferManager {

    private static final int BATCH_SIZE = 10_000;
    private static final int PROGRESS_EVERY = 100_000;

    /**
     * Full table copy: drop/create on target, batched insert from source.
     *
     * @param source    home worker, e.g. worker2 for lineitem
     * @param target    worker that needs a local copy, e.g. worker1
     * @param tableName base or intermediate table name
     * @return total transfer time in milliseconds
     */
    public long transfer(
            WorkerNode source,
            WorkerNode target,
            String tableName
    ) throws Exception {

        long start =
                System.currentTimeMillis();

        System.out.println(
                "Copying \""
                        + tableName
                        + "\" from "
                        + source.name()
                        + " to "
                        + target.name()
                        + " ..."
        );
        System.out.flush();

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
                            "SELECT * FROM \""
                                    + tableName
                                    + "\""
                    );

            ResultSetMetaData md =
                    rs.getMetaData();

            int cols =
                    md.getColumnCount();

            String createSql =
                    buildCreateSql(
                            tableName,
                            md,
                            cols
                    );

            dst.createStatement()
                    .execute(
                            "DROP TABLE IF EXISTS \""
                                    + tableName
                                    + "\""
                    );

            dst.createStatement()
                    .execute(createSql);

            PreparedStatement ps =
                    dst.prepareStatement(
                            buildInsertSql(
                                    tableName,
                                    cols
                            )
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

                if (rowCount % BATCH_SIZE == 0) {
                    ps.executeBatch();
                    ps.clearBatch();

                    if (rowCount % PROGRESS_EVERY == 0) {
                        System.out.println(
                                "  ... "
                                        + rowCount
                                        + " rows copied"
                        );
                        System.out.flush();
                    }
                }
            }

            ps.executeBatch();
            ps.clearBatch();
            dst.commit();

            long duration =
                    System.currentTimeMillis()
                            - start;

            System.out.println(
                    "Done: "
                            + rowCount
                            + " rows in "
                            + duration
                            + " ms"
            );
            System.out.flush();

            return duration;
        }
    }

    /**
     * @param tableName destination table
     * @param md        source result metadata
     * @param cols      column count
     * @return CREATE TABLE DDL matching source column types
     */
    private static String buildCreateSql(
            String tableName,
            ResultSetMetaData md,
            int cols
    ) throws Exception {

        StringBuilder create =
                new StringBuilder();

        create.append(
                "CREATE TABLE \""
                        + tableName
                        + "\" ("
        );

        for (int i = 1; i <= cols; i++) {
            create.append(md.getColumnName(i))
                    .append(" ")
                    .append(md.getColumnTypeName(i));

            if (i < cols) {
                create.append(",");
            }
        }

        create.append(")");

        return create.toString();
    }

    /**
     * @param tableName destination table
     * @param cols      number of columns
     * @return parameterized INSERT statement with {@code ?} placeholders
     */
    private static String buildInsertSql(
            String tableName,
            int cols
    ) {
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

        return insertSql.toString();
    }
}
