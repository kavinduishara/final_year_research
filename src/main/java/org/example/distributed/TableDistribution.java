package org.example.distributed;



import java.util.HashMap;

import java.util.HashSet;

import java.util.List;

import java.util.Map;

import java.util.Set;



/**

 * Maps each base table to its home worker for distributed Q1-style queries.

 *

 * <p>Typical research setup:

 * <pre>

 *   worker1 → {customer, orders}

 *   worker2 → {lineitem}

 *

 *   workerFor("customer")  → "worker1"

 *   workerFor("lineitem")  → "worker2"

 *   isFullyLocal({customer, orders}, "worker1") → true

 *   spansWorkers({customer, lineitem})           → true

 * </pre>

 */

public class TableDistribution {


 
    /** Lowercase table name → worker name (e.g. "orders" → "worker1"). */

    private final Map<String, String> tableToWorker =

            new HashMap<>();



    /**

     * Builds the map from each worker's declared table set in {@link WorkerRegistry}.

     *

     * @param workers configured nodes, e.g. [worker1, worker2]

     */

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



    /**

     * @param table e.g. "lineitem"

     * @return home worker name, e.g. "worker2", or null if unknown

     */

    public String workerFor(String table) {

        return tableToWorker.get(

                table.toLowerCase()

        );

    }



    /**

     * @param workerName e.g. "worker1"

     * @return all tables hosted on that worker, e.g. {customer, orders}

     */

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



    /**

     * Counts how many fragment tables are local on a worker (for assignment tie-break).

     *

     * @param fragmentTables e.g. {customer, orders, lineitem}

     * @param workerName     candidate worker

     * @return overlap count, e.g. 2 for worker1 on Q1 full join

     */

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



    /**

     * @param fragmentTables tables in a fragment subtree

     * @return {@code true} if those tables live on more than one worker

     */

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



    /**

     * @param fragmentTables tables required by a fragment

     * @param workerName     proposed execution worker

     * @return {@code true} if every table's home is {@code workerName}

     */

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



    /** Prints table → worker mapping to stdout for debugging. */

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

