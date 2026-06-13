package org.example.distributed;

import org.example.exec.ExecutionMetrics;
import org.example.plan.CutCandidate;
import org.example.sql.FragmentSql;

public class DistributedExecutor {

    public DistributedExecutionResult execute(
            FragmentSql sql,
            CutCandidate cut,
            TableDistribution distribution
    ) throws Exception {

        System.out.println(
                "\n===== DISTRIBUTED EXECUTION ====="
        );

        WorkerNode worker1 =
                WorkerRegistry.byName(
                        cut.fragment1Worker()
                );

        WorkerNode worker2 =
                WorkerRegistry.byName(
                        cut.fragment2Worker()
                );

        IntermediateTableCleaner.dropIfExists(
                worker1,
                sql.tempTableName()
        );

        if (!cut.fragment1Worker().equals(
                cut.fragment2Worker()
        )) {
            IntermediateTableCleaner.dropIfExists(
                    worker2,
                    sql.tempTableName()
            );
        }

        BaseTableShipper shipper =
                new BaseTableShipper();

        long baseTableShipTime =
                shipper.shipBeforeExecution(
                        cut,
                        distribution
                );

        WorkerExecutor executor =
                new WorkerExecutor();

        System.out.println(
                "Fragment1 -> "
                        + worker1.name()
                        + " (tables: "
                        + worker1.tables()
                        + ")"
        );

        long fragment1Time =
                executor.executeDrop(
                        worker1,
                        sql.dropSql()
                )
                        + executor.executeCreate(
                        worker1,
                        sql.createSql()
                );

        long intermediateTransferTime = 0;

        if (!cut.fragment1Worker().equals(
                cut.fragment2Worker()
        )) {
            System.out.println(
                    "\nTransfer INTERMEDIATE "
                            + sql.tempTableName()
                            + " -> "
                            + worker2.name()
            );

            TransferManager transferManager =
                    new TransferManager();

            intermediateTransferTime =
                    transferManager.transfer(
                            worker1,
                            worker2,
                            sql.tempTableName()
                    );
        }
        else {
            System.out.println(
                    "\nNo intermediate transfer (both fragments on "
                            + worker1.name()
                            + ")"
            );
        }

        System.out.println(
                "\nFragment2 -> "
                        + worker2.name()
                        + " (tables: "
                        + worker2.tables()
                        + ")"
        );

        WorkerExecutor.QueryExecutionResult fragment2 =
                executor.executeQueryCollect(
                        worker2,
                        sql.sql2()
                );

        ExecutionMetrics metrics =
                new ExecutionMetrics(
                        fragment1Time,
                        baseTableShipTime,
                        intermediateTransferTime,
                        fragment2.executionTimeMs()
                );

        printMetrics(metrics);

        return new DistributedExecutionResult(
                metrics,
                fragment2.data().columnNames(),
                fragment2.data().rows()
        );
    }

    private static void printMetrics(
            ExecutionMetrics metrics
    ) {
        System.out.println(
                "\n===== EXECUTION METRICS ====="
        );

        System.out.println(
                "Fragment1 Time       = "
                        + metrics.fragment1TimeMs()
                        + " ms"
        );

        System.out.println(
                "Base Table Ship Time = "
                        + metrics.baseTableShipTimeMs()
                        + " ms"
        );

        System.out.println(
                "Intermediate Transfer= "
                        + metrics.intermediateTransferTimeMs()
                        + " ms"
        );

        System.out.println(
                "Fragment2 Time       = "
                        + metrics.fragment2TimeMs()
                        + " ms"
        );

        System.out.println(
                "Runtime (F1+F2)      = "
                        + metrics.runtimeMs()
                        + " ms"
        );

        System.out.println(
                "Total Transfer       = "
                        + metrics.transferTimeMs()
                        + " ms"
        );

        System.out.println(
                "Total Time           = "
                        + metrics.totalTimeMs()
                        + " ms"
        );

        System.out.println(
                "Reward input         = "
                        + -(metrics.runtimeMs()
                        + metrics.transferTimeMs())
        );
    }
}
