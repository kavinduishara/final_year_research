package org.example.distributed;

import org.example.config.AppConfig;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Loads worker PostgreSQL nodes from application.properties (singleton cache).
 *
 * <p>Example config → workers:
 * <pre>
 *   worker.db.urls = jdbc:...5434/w1, jdbc:...5435/w2
 *   worker1.tables = customer,orders
 *   worker2.tables = lineitem
 *
 *   worker1 → {customer, orders} on port 5434
 *   worker2 → {lineitem} on port 5435
 * </pre>
 */
public class WorkerRegistry {

    private static List<WorkerNode> cachedWorkers;

    /**
     * @return [worker1, worker2] loaded once from config
     */
    public static List<WorkerNode> workers() {
        if (cachedWorkers == null) {
            cachedWorkers = loadWorkers();
        }
        return cachedWorkers;
    }

    /**
     * @return TableDistribution mapping customer→worker1, orders→worker1, lineitem→worker2
     */
    public static TableDistribution tableDistribution() {
        return new TableDistribution(
                workers()
        );
    }

    /**
     * @param name e.g. "worker1" or "worker2"
     * @return matching WorkerNode
     * @throws IllegalArgumentException if name unknown
     */
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

    /** Reads worker.db.urls, worker1.tables, worker2.tables from application.properties. */
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

    /** @param csv e.g. "customer,orders" → Set{"customer", "orders"} */
    private static Set<String> parseTables(
            String csv
    ) {
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }
}
