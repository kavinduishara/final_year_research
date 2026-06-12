package org.example.distributed;

import java.util.List;

public class WorkerRegistry {

    public static List<WorkerNode> workers() {

        return List.of(

                new WorkerNode(
                        "worker1",
                        "jdbc:postgresql://10.128.0.3:5432/w1",
                        "postgres",
                        "0000"
                ),

                new WorkerNode(
                        "worker2",
                        "jdbc:postgresql://10.128.0.4:5432/w2",
                        "postgres",
                        "0000"
                )
        );
    }
}