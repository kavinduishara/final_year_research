package org.example.distributed;

import java.util.List;

public class WorkerRegistry {

    public static List<WorkerNode> workers() {

        return List.of(

                new WorkerNode(
                        "worker1",
                        "jdbc:postgresql://localhost:5434/w1",
                        "postgres",
                        "0000"
                ),

                new WorkerNode(
                        "worker2",
                        "jdbc:postgresql://localhost:5435/w2",
                        "postgres",
                        "0000"
                )
        );
    }
}