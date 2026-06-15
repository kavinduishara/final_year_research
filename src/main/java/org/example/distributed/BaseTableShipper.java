package org.example.distributed;

import org.example.config.ResearchSettings;
import org.example.plan.CutCandidate;

import java.util.Set;

/**
 * Ships base tables to the worker assigned to each query fragment.
 *
 * <p>Q1 example — cut@42 with worker1={customer,orders}, worker2={lineitem}:
 * <ul>
 *   <li>Fragment1 on worker1: no base shipping (tables are local)</li>
 *   <li>Fragment2 on worker2: no base shipping (lineitem is local)</li>
 * </ul>
 * Cut@38 (shallow): fragment2 needs lineitem on worker1 → ships lineitem worker2→worker1.
 *
 * <p>Returns total shipping time in milliseconds for {@link org.example.exec.ExecutionMetrics}.
 */
public class BaseTableShipper {

    private final TransferManager transferManager =
            new TransferManager();

    /**
     * Ships every remote base table required by one fragment to its assigned worker.
     *
     * @param tables         fragment tables, e.g. {lineitem}
     * @param targetWorker   assigned worker, e.g. "worker1"
     * @param distribution   home-worker map
     * @return cumulative transfer time ms for all shipped tables
     */
    public long shipForFragment(
            Set<String> tables,
            String targetWorker,
            TableDistribution distribution
    ) throws Exception {

        long total = 0;

        for (String table : tables) {
            String homeWorker =
                    distribution.workerFor(table);

            if (homeWorker == null) {
                throw new IllegalStateException(
                        "No home worker for table: "
                                + table
                );
            }

            if (homeWorker.equals(targetWorker)) {
                continue;
            }

            WorkerNode target =
                    WorkerRegistry.byName(
                            targetWorker
                    );

            if (ResearchSettings.skipShipIfPresent()
                    && WorkerTableProbe.hasRows(
                    target,
                    table
            )) {
                System.out.println(
                        "Skip shipping "
                                + table
                                + " (already on "
                                + targetWorker
                                + ")"
                );
                continue;
            }

            System.out.println(
                    "\nShipping base table "
                            + table
                            + ": "
                            + homeWorker
                            + " -> "
                            + targetWorker
            );
            System.out.flush();

            total +=
                    transferManager.transfer(
                            WorkerRegistry.byName(
                                    homeWorker
                            ),
                            target,
                            table
                    );
        }

        return total;
    }

    /**
     * Ships base tables for both fragments of a chosen cut before execution.
     *
     * @param cut          enriched candidate, e.g. cut@38 fragment1Worker=worker1
     * @param distribution table placement
     * @return total base-table shipping time in ms
     */
    public long shipBeforeExecution(
            CutCandidate cut,
            TableDistribution distribution
    ) throws Exception {

        long total = 0;

        total +=
                shipForFragment(
                        cut.fragment1Tables(),
                        cut.fragment1Worker(),
                        distribution
                );

        total +=
                shipForFragment(
                        cut.fragment2Tables(),
                        cut.fragment2Worker(),
                        distribution
                );

        return total;
    }
}
