package org.example.distributed;

import org.example.sql.FragmentSql;

public class DistributedExecutor {

    public long execute(
            FragmentSql sql,
            String fragment1WorkerName,
            String fragment2WorkerName
    ) throws Exception {

        System.out.println(
                "\n===== DISTRIBUTED EXECUTION ====="
        );

        WorkerNode worker1 =
                WorkerRegistry.byName(
                        fragment1WorkerName
                );

        WorkerNode worker2 =
                WorkerRegistry.byName(
                        fragment2WorkerName
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
                executor.executeUpdate(
                        worker1,
                        sql.sql1()
                );

        long transferTime = 0;

        if (!fragment1WorkerName.equals(
                fragment2WorkerName
        )) {
            System.out.println(
                    "\nTransfer INTERMEDIATE "
                            + sql.tempTableName()
                            + " -> "
                            + worker2.name()
            );

            TransferManager transferManager =
                    new TransferManager();

            transferTime =
                    transferManager.transfer(
                            worker1,
                            worker2,
                            sql.tempTableName()
                    );
        }
        else {
            System.out.println(
                    "\nNo transfer needed (both fragments on "
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

        long fragment2Time =
                executor.executeQuery(
                        worker2,
                        sql.sql2()
                );

        long total =
                fragment1Time
                        + transferTime
                        + fragment2Time;

        System.out.println(
                "\n===== EXECUTION METRICS ====="
        );

        System.out.println(
                "Fragment1 Time = "
                        + fragment1Time
                        + " ms"
        );

        System.out.println(
                "Transfer Time  = "
                        + transferTime
                        + " ms"
        );

        System.out.println(
                "Fragment2 Time = "
                        + fragment2Time
                        + " ms"
        );

        System.out.println(
                "Total Time     = "
                        + total
                        + " ms"
        );

        return total;
    }
}
