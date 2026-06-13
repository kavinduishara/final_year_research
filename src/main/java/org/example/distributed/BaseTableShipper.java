package org.example.distributed;

import org.example.config.ResearchSettings;
import org.example.plan.CutCandidate;

import java.util.Set;

public class BaseTableShipper {

    private final TransferManager transferManager =
            new TransferManager();

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
