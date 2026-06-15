package org.example.distributed;

import java.util.Set;

/**
 * Configuration record for one PostgreSQL worker in the research cluster.
 *
 * <p>Example two-worker Q1 setup:
 * <pre>
 *   worker1 = WorkerNode(
 *     "worker1",
 *     "jdbc:postgresql://localhost:5433/tpch_worker1",
 *     "postgres", "secret",
 *     Set.of("customer", "orders")
 *   )
 *   worker2 = WorkerNode(
 *     "worker2",
 *     "jdbc:postgresql://localhost:5434/tpch_worker2",
 *     "postgres", "secret",
 *     Set.of("lineitem")
 *   )
 * </pre>
 *
 * @param name     logical id used in cut assignment, e.g. "worker1"
 * @param jdbcUrl  PostgreSQL connection URL for this worker's database
 * @param user     database user
 * @param password database password
 * @param tables   base tables physically stored on this worker
 */
public record WorkerNode(
        String name,
        String jdbcUrl,
        String user,
        String password,
        Set<String> tables
) {
}
