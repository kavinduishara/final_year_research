package org.example.distributed;

import org.example.sql.FragmentSql;

public class DistributedExecutor {

    public long execute(FragmentSql sql) throws Exception {

        System.out.println(
                "\n===== DISTRIBUTED EXECUTION ====="
        );

        WorkerNode worker1 =
                WorkerRegistry.workers().get(0);

        WorkerNode worker2 =
                WorkerRegistry.workers().get(1);

        WorkerExecutor executor =
                new WorkerExecutor();

        System.out.println(
                "Fragment1 -> worker1"
        );

        long fragment1Time =
                executor.executeUpdate(
                        worker1,
                        sql.sql1()
                );

        System.out.println(
                "\nTransfer INTERMEDIATE -> worker2"
        );

        TransferManager transferManager =
                new TransferManager();

        long transferTime =
                transferManager.transfer(
                        worker1,
                        worker2,
                        "INTERMEDIATE_17"
                );

        System.out.println(
                "\nFragment2 -> worker2"
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