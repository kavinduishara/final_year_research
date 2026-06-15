package org.example.distributed;

import org.example.config.ResearchSettings;
import org.example.plan.CutCandidate;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Resets workers to the configured data placement for fair research runs.
 *
 * <p>Between Q1 benchmark queries, removes:
 * <ul>
 *   <li>Shipped base-table copies (e.g. lineitem copied worker2→worker1)</li>
 *   <li>Leftover intermediate tables matching {@code inter_%}</li>
 * </ul>
 *
 * <p>Example after Q1 cut@42:
 * <pre>
 *   cleanupAfterExecution(cut, "inter_q1_n42")
 *   → DROP "inter_q1_n42" on worker1 and worker2
 *   → DROP shipped "lineitem" from worker1 (home=worker2)
 * </pre>
 */
public final class ResearchEnvironmentCleaner {

    private ResearchEnvironmentCleaner() {
    }

    /**
     * Runs {@link #cleanupAll()} when {@code research.cleanup-between-queries=true}.
     */
    public static void cleanupBetweenQueries() {

        if (!ResearchSettings.cleanupBetweenQueries()) {
            return;
        }

        cleanupAll();
    }

    /**
     * Full reset on every worker: drop shipped tables and all {@code inter_*} tables.
     */
    public static void cleanupAll() {

        TableDistribution distribution =
                WorkerRegistry.tableDistribution();

        List<WorkerNode> workers =
                WorkerRegistry.workers();

        Set<String> allTables =
                allKnownTables(workers);

        for (WorkerNode worker : workers) {

            dropShippedBaseTables(
                    worker,
                    allTables,
                    distribution
            );

            dropIntermediateTables(worker);
        }
    }

    /**
     * Post-run cleanup for one Q1 execution (intermediate + shipped base tables).
     *
     * @param cut                   chosen cut, e.g. node 42 on worker1/worker2
     * @param intermediateTableName e.g. "inter_single_n42"
     */
    public static void cleanupAfterExecution(
            CutCandidate cut,
            String intermediateTableName
    ) {

        if (!ResearchSettings.cleanupAfterExecution()) {
            return;
        }

        WorkerNode worker1 =
                WorkerRegistry.byName(
                        cut.fragment1Worker()
                );

        IntermediateTableCleaner.dropIfExists(
                worker1,
                intermediateTableName
        );

        if (!cut.fragment1Worker().equals(
                cut.fragment2Worker()
        )) {
            WorkerNode worker2 =
                    WorkerRegistry.byName(
                            cut.fragment2Worker()
                    );

            IntermediateTableCleaner.dropIfExists(
                    worker2,
                    intermediateTableName
            );
        }

        resetShippedBaseTables();
    }

    private static void resetShippedBaseTables() {

        TableDistribution distribution =
                WorkerRegistry.tableDistribution();

        List<WorkerNode> workers =
                WorkerRegistry.workers();

        Set<String> allTables =
                allKnownTables(workers);

        for (WorkerNode worker : workers) {
            dropShippedBaseTables(
                    worker,
                    allTables,
                    distribution
            );
        }
    }

    private static Set<String> allKnownTables(
            List<WorkerNode> workers
    ) {
        Set<String> tables =
                new HashSet<>();

        for (WorkerNode worker : workers) {
            tables.addAll(worker.tables());
        }

        return tables;
    }

    private static void dropShippedBaseTables(
            WorkerNode worker,
            Set<String> allTables,
            TableDistribution distribution
    ) {
        for (String table : allTables) {

            String homeWorker =
                    distribution.workerFor(table);

            if (homeWorker == null) {
                continue;
            }

            if (homeWorker.equals(worker.name())) {
                continue;
            }

            if (!WorkerTableProbe.hasRows(
                    worker,
                    table
            )) {
                continue;
            }

            System.out.println(
                    "Cleanup: dropping shipped table \""
                            + table
                            + "\" from "
                            + worker.name()
                            + " (home="
                            + homeWorker
                            + ")"
            );

            IntermediateTableCleaner.dropIfExists(
                    worker,
                    table
            );
        }
    }

    private static void dropIntermediateTables(
            WorkerNode worker
    ) {
        for (String tableName :
                listIntermediateTables(worker)) {

            System.out.println(
                    "Cleanup: dropping intermediate \""
                            + tableName
                            + "\" from "
                            + worker.name()
            );

            IntermediateTableCleaner.dropIfExists(
                    worker,
                    tableName
            );
        }
    }

    private static List<String> listIntermediateTables(
            WorkerNode worker
    ) {
        List<String> names =
                new ArrayList<>();

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
                                """
                                SELECT tablename
                                FROM pg_tables
                                WHERE schemaname = 'public'
                                  AND tablename LIKE 'inter_%'
                                """
                        )
        ) {
            while (rs.next()) {
                names.add(
                        rs.getString(1)
                );
            }
        }
        catch (Exception e) {
            System.out.println(
                    "Warning: could not list intermediate tables on "
                            + worker.name()
                            + ": "
                            + e.getMessage()
            );
        }

        return names;
    }
}
