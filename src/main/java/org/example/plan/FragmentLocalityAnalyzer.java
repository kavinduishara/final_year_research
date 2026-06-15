package org.example.plan;

import org.apache.calcite.rel.RelNode;
import org.example.config.ResearchSettings;
import org.example.distributed.TableDistribution;
import org.example.distributed.WorkerNode;
import org.example.qos.TransferCostEstimator;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Enriches raw {@link CutCandidate} statistics with data-locality and transfer costs.
 *
 * <p>For each JOIN cut, determines which tables belong to fragment1 (subtree below the cut)
 * vs fragment2 (remaining tables), assigns workers, and estimates shipping cost.
 *
 * <p>Example Q1 (TPC-H style):
 * <pre>
 *   SQL: SELECT c.mktsegment, SUM(o.totalprice)
 *        FROM customer c JOIN orders o ... JOIN lineitem l ...
 *
 *   Distribution: worker1 = {customer, orders}, worker2 = {lineitem}
 *
 *   Cut at node 38 (customer ⋈ orders):
 *     fragment1Tables = {customer, orders}  → worker1 (fully local)
 *     fragment2Tables = {lineitem}          → worker2
 *     intermediateTransfer = estimate(rows@38) because workers differ
 *
 *   Cut at node 42 (deeper join):
 *     fragment1Tables = {customer, orders, lineitem}
 *     localityBucket may be CROSS_SHARD or SHIPPING_REQUIRED
 * </pre>
 */
public class FragmentLocalityAnalyzer {

    /**
     * Adds locality, worker assignment, transfer costs, and executability to one cut.
     *
     * @param root           best plan root (Q1 aggregate)
     * @param cutNode        JOIN RelNode at this cut (e.g. Join#38)
     * @param base           statistics-only candidate from {@link PlanStatisticsCollector}
     * @param distribution   table → home worker map (customer→worker1, lineitem→worker2)
     * @param workers        configured worker nodes [worker1, worker2]
     * @param shippingEnabled whether base tables may be copied to non-home workers
     * @return enriched candidate, e.g. fragment1Worker="worker1", totalTransfer≈1.5e8 bytes
     */
    public static CutCandidate enrich(
            RelNode root,
            RelNode cutNode,
            CutCandidate base,
            TableDistribution distribution,
            List<WorkerNode> workers,
            boolean shippingEnabled
    ) {

        // ex sql : SELECT c.mktsegment, SUM(o.totalprice)
        // FROM customer c
        // JOIN orders o ON c.custkey = o.custkey
        // JOIN lineitem l ON o.orderkey = l.orderkey
        // WHERE l.shipdate > DATE '1999-05-01'
        // GROUP BY c.mktsegment

        Set<String> fragment1Tables =
                RelTableCollector.collect(cutNode);
                

        Set<String> allTables =
                RelTableCollector.collect(root);


        Set<String> fragment2Tables =
                new HashSet<>(allTables);
                                

        fragment2Tables.removeAll(fragment1Tables);

        String fragment1Worker =
                assignWorker(
                        fragment1Tables,
                        distribution,
                        workers
                );

        String fragment2Worker =
                fragment2Tables.isEmpty()
                        ? fragment1Worker
                        : assignWorker(
                        fragment2Tables,
                        distribution,
                        workers
                );

        Map<String, Double> tableRows =
                TableRowEstimator.estimate(root);

        double baseTableTransfer =
                remoteTableTransferCost(
                        fragment1Tables,
                        fragment1Worker,
                        distribution,
                        tableRows
                )
                        + remoteTableTransferCost(
                        fragment2Tables,
                        fragment2Worker,
                        distribution,
                        tableRows
                );

        double intermediateTransfer = 0;

        if (!fragment1Worker.equals(
                fragment2Worker
        )) {
            intermediateTransfer =
                    TransferCostEstimator.estimate(
                            base.estimatedRows()
                    );
        }

        double totalTransfer =
                baseTableTransfer
                        + intermediateTransfer;

        boolean executable =
                isExecutable(
                        fragment1Tables,
                        fragment1Worker,
                        fragment2Tables,
                        fragment2Worker,
                        distribution,
                        shippingEnabled
                );

        String localityBucket =
                classifyLocality(
                        fragment1Tables,
                        distribution,
                        fragment1Worker,
                        fragment2Worker,
                        baseTableTransfer,
                        executable,
                        shippingEnabled
                );

        double baselineDecisionCost =
                org.example.qos.BaselineCostEstimator
                        .decisionCost(
                                base,
                                fragment1Tables,
                                fragment1Worker,
                                fragment2Worker,
                                baseTableTransfer,
                                intermediateTransfer,
                                totalTransfer,
                                tableRows
                        );

        return new CutCandidate(
                base.nodeId(),
                base.estimatedRows(),
                base.depth(),
                base.estimatedCost(),
                fragment1Tables,
                fragment2Tables,
                fragment1Worker,
                fragment2Worker,
                baseTableTransfer,
                intermediateTransfer,
                totalTransfer,
                baselineDecisionCost,
                localityBucket,
                executable
        );
    }

    /**
     * Picks the worker that can run a fragment with minimal data movement.
     *
     * <p>Prefers a worker where all fragment tables are local; otherwise picks
     * the worker with the largest table overlap.
     *
     * @param fragmentTables tables in the fragment, e.g. {customer, orders}
     * @param distribution   home-worker lookup
     * @param workers        available workers
     * @return worker name, e.g. "worker1"
     */
    private static String assignWorker(
            Set<String> fragmentTables,
            TableDistribution distribution,
            List<WorkerNode> workers
    ) {
        if (fragmentTables.isEmpty()) {
            throw new IllegalStateException(
                    "Cannot assign worker for empty fragment"
            );
        }

        return workers.stream()
                .filter(w ->
                        distribution.isFullyLocal(
                                fragmentTables,
                                w.name()
                        )
                )
                .map(WorkerNode::name)
                .findFirst()
                .orElseGet(() ->
                        workers.stream()
                                .max(
                                        Comparator.comparingInt(
                                                w ->
                                                        distribution.overlap(
                                                                fragmentTables,
                                                                w.name()
                                                        )
                                        )
                                )
                                .map(WorkerNode::name)
                                .orElseThrow()
                );
    }

    /**
     * Whether both fragments can run under current shipping policy.
     *
     * @return {@code true} if every table has a home worker and (when shipping
     *         is disabled) each fragment is fully local on its assigned worker
     */
    private static boolean isExecutable(
            Set<String> fragment1Tables,
            String fragment1Worker,
            Set<String> fragment2Tables,
            String fragment2Worker,
            TableDistribution distribution,
            boolean shippingEnabled
    ) {
        if (!allTablesHaveHome(
                fragment1Tables,
                distribution
        )) {
            return false;
        }

        if (!allTablesHaveHome(
                fragment2Tables,
                distribution
        )) {
            return false;
        }

        if (shippingEnabled) {
            return true;
        }

        if (!distribution.isFullyLocal(
                fragment1Tables,
                fragment1Worker
        )) {
            return false;
        }

        if (fragment2Tables.isEmpty()) {
            return true;
        }

        return distribution.isFullyLocal(
                fragment2Tables,
                fragment2Worker
        );
    }

    /** @return {@code false} if any table has no configured home worker */
    private static boolean allTablesHaveHome(
            Set<String> tables,
            TableDistribution distribution
    ) {
        for (String table : tables) {
            if (distribution.workerFor(table) == null) {
                return false;
            }
        }

        return true;
    }

    /**
     * Sum of transfer-cost estimates for tables not local on the assigned worker.
     *
     * @param fragmentTables  e.g. {lineitem} on worker1
     * @param assignedWorker  e.g. "worker1" (home of lineitem is worker2)
     * @param tableRows       row counts from {@link TableRowEstimator}
     * @return bytes estimate, e.g. 6_000_000 × 100 = 600_000_000 for lineitem
     */
    private static double remoteTableTransferCost(
            Set<String> fragmentTables,
            String assignedWorker,
            TableDistribution distribution,
            Map<String, Double> tableRows
    ) {
        double cost = 0;

        for (String table : fragmentTables) {
            String homeWorker =
                    distribution.workerFor(table);

            if (homeWorker != null
                    && !homeWorker.equals(
                    assignedWorker
            )) {
                cost +=
                        TransferCostEstimator.estimate(
                                tableRows.getOrDefault(
                                        table,
                                        10_000.0
                                )
                        );
            }
        }

        return cost;
    }

    /**
     * Labels how data is placed relative to workers (used as bandit features).
     *
     * @return bucket name, e.g. "INTERMEDIATE_ONLY" when cut@38 ships only
     *         intermediate rows worker1→worker2, not base tables
     */
    private static String classifyLocality(
            Set<String> fragment1Tables,
            TableDistribution distribution,
            String fragment1Worker,
            String fragment2Worker,
            double baseTableTransfer,
            boolean executable,
            boolean shippingEnabled
    ) {
        if (!executable) {
            return "INFEASIBLE";
        }

        if (baseTableTransfer > 0 && shippingEnabled) {
            return "SHIPPING_REQUIRED";
        }

        if (distribution.spansWorkers(
                fragment1Tables
        )) {
            return "CROSS_SHARD";
        }

        if (baseTableTransfer > 0) {
            return "PARTIAL_SHIPPING";
        }

        if (fragment1Worker.equals(
                fragment2Worker
        )) {
            return "SINGLE_WORKER";
        }

        return "INTERMEDIATE_ONLY";
    }

    /**
     * Depth-first search for a RelNode by Calcite id.
     *
     * @param root plan root
     * @param id   target id, e.g. 42
     * @return matching node or {@code null}
     */
    private static RelNode findById(
            RelNode root,
            int id
    ) {
        if (root.getId() == id) {
            return root;
        }

        for (RelNode input : root.getInputs()) {
            RelNode found =
                    findById(
                            input,
                            id
                    );

            if (found != null) {
                return found;
            }
        }

        return null;
    }

    /**
     * Enriches every base candidate for a plan (batch wrapper around {@link #enrich}).
     *
     * @param root         Q1 best plan
     * @param bases        candidates from {@link PlanStatisticsCollector}, e.g. [cut#38, cut#42]
     * @param distribution worker1/worker2 table placement
     * @param workers      worker list
     * @return fully enriched candidates ready for policy selection
     */
    public static List<CutCandidate> enrichAll(
            RelNode root,
            List<CutCandidate> bases,
            TableDistribution distribution,
            List<WorkerNode> workers
    ) {
        boolean shippingEnabled =
                ResearchSettings.shippingEnabled();

        System.out.println(
                "\nShipping enabled = "
                        + shippingEnabled
        );

        return bases.stream()
                .map(base -> {
                    RelNode cutNode =
                            findById(
                                    root,
                                    base.nodeId()
                            );

                    if (cutNode == null) {
                        throw new IllegalStateException(
                                "Cut node not found: "
                                        + base.nodeId()
                        );
                    }

                    return enrich(
                            root,
                            cutNode,
                            base,
                            distribution,
                            workers,
                            shippingEnabled
                    );
                })
                .toList();
    }
}
