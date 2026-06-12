package org.example.distributed;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TableDistribution {

    private final Map<String, String> tableToWorker =
            new HashMap<>();

    public TableDistribution(List<WorkerNode> workers) {
        for (WorkerNode worker : workers) {
            for (String table : worker.tables()) {
                tableToWorker.put(
                        table.toLowerCase(),
                        worker.name()
                );
            }
        }
    }

    public String workerFor(String table) {
        return tableToWorker.get(
                table.toLowerCase()
        );
    }

    public Set<String> tablesOn(String workerName) {
        Set<String> tables = new HashSet<>();

        tableToWorker.forEach(
                (table, worker) -> {
                    if (worker.equals(workerName)) {
                        tables.add(table);
                    }
                }
        );

        return tables;
    }

    public int overlap(
            Set<String> fragmentTables,
            String workerName
    ) {
        int count = 0;

        for (String table : fragmentTables) {
            if (workerName.equals(
                    workerFor(table)
            )) {
                count++;
            }
        }

        return count;
    }

    public boolean spansWorkers(
            Set<String> fragmentTables
    ) {
        String first = null;

        for (String table : fragmentTables) {
            String worker =
                    workerFor(table);

            if (worker == null) {
                continue;
            }

            if (first == null) {
                first = worker;
            }
            else if (!first.equals(worker)) {
                return true;
            }
        }

        return false;
    }

    public boolean isFullyLocal(
            Set<String> fragmentTables,
            String workerName
    ) {
        if (fragmentTables.isEmpty()) {
            return true;
        }

        for (String table : fragmentTables) {
            if (!workerName.equals(
                    workerFor(table)
            )) {
                return false;
            }
        }

        return true;
    }

    public void print() {
        System.out.println(
                "\n===== DATA DISTRIBUTION ====="
        );

        tableToWorker.forEach(
                (table, worker) ->
                        System.out.println(
                                table
                                        + " -> "
                                        + worker
                        )
        );
    }
}
