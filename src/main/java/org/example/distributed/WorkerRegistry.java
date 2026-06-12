package org.example.distributed;

import org.example.config.AppConfig;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class WorkerRegistry {

    private static List<WorkerNode> cachedWorkers;

    public static List<WorkerNode> workers() {
        if (cachedWorkers == null) {
            cachedWorkers = loadWorkers();
        }
        return cachedWorkers;
    }

    public static TableDistribution tableDistribution() {
        return new TableDistribution(
                workers()
        );
    }

    public static WorkerNode byName(String name) {
        return workers().stream()
                .filter(w -> w.name().equals(name))
                .findFirst()
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "Unknown worker: "
                                        + name
                        )
                );
    }

    private static List<WorkerNode> loadWorkers() {
        AppConfig cfg =
                new AppConfig(
                        "/application.properties"
                );

        String user =
                cfg.get("worker.db.user");

        String pass =
                cfg.get("worker.db.pass");

        String[] urls =
                cfg.get("worker.db.urls")
                        .split(",");

        return List.of(
                new WorkerNode(
                        "worker1",
                        urls[0].trim(),
                        user,
                        pass,
                        parseTables(
                                cfg.get(
                                        "worker1.tables"
                                )
                        )
                ),
                new WorkerNode(
                        "worker2",
                        urls[1].trim(),
                        user,
                        pass,
                        parseTables(
                                cfg.get(
                                        "worker2.tables"
                                )
                        )
                )
        );
    }

    private static Set<String> parseTables(
            String csv
    ) {
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }
}
