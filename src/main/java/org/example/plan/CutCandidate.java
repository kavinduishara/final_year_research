package org.example.plan;

import java.util.Set;

/**
 * One candidate JOIN cut with planner stats + distributed placement info.
 *
 * <p>Example for Q1, cut at node 42 (orders⋈lineitem join):
 * <pre>
 *   nodeId                  = 42
 *   estimatedRows           = 1500000.0
 *   depth                   = 2
 *   estimatedCost           = 45000.0
 *   fragment1Tables         = {customer, orders, lineitem}  (subtree below cut)
 *   fragment2Tables         = {}                              (often empty = rest of plan)
 *   fragment1Worker         = "worker2"   (has lineitem home)
 *   fragment2Worker         = "worker2"
 *   baseTableTransferCost   = 120000.0    (ship customer+orders to worker2)
 *   intermediateTransferCost= 0.0         (same worker)
 *   totalTransferCost       = 120000.0
 *   baselineDecisionCost    = 2400.0      (weighted-ship estimate)
 *   localityBucket          = "SHIPPING_REQUIRED"
 *   executable              = true
 * </pre>
 */
public record CutCandidate(
        /** Calcite RelNode id of this JOIN cut. */
        int nodeId,
        /** Calcite metadata row estimate at cut point. */
        double estimatedRows,
        /** Depth of join in plan tree (deeper = later in join order). */
        int depth,
        /** Calcite planner self-cost (rows), or -1 if unavailable. */
        double estimatedCost,
        /** Base tables in fragment1 (subtree below cut). */
        Set<String> fragment1Tables,
        /** Base tables in fragment2 (full plan minus fragment1 tables). */
        Set<String> fragment2Tables,
        /** Worker assigned to run fragment1 (CREATE intermediate table). */
        String fragment1Worker,
        /** Worker assigned to run fragment2 (final SELECT). */
        String fragment2Worker,
        /** Estimated cost to ship base tables to assigned workers. */
        double baseTableTransferCost,
        /** Estimated cost to transfer intermediate result (0 if same worker). */
        double intermediateTransferCost,
        /** baseTableTransferCost + intermediateTransferCost. */
        double totalTransferCost,
        /** Cost used by baseline policy to rank cuts. */
        double baselineDecisionCost,
        /** e.g. SINGLE_WORKER, INTERMEDIATE_ONLY, SHIPPING_REQUIRED, INFEASIBLE. */
        String localityBucket,
        /** false if required tables cannot be placed/shipped. */
        boolean executable
) {
    /**
     * Minimal constructor before FragmentLocalityAnalyzer enrichment.
     * Sets workers/transfer/locality to empty defaults.
     *
     * @param nodeId         e.g. 42
     * @param estimatedRows  e.g. 1500000.0
     * @param depth          e.g. 2
     * @param estimatedCost  e.g. 45000.0
     */
    public CutCandidate(
            int nodeId,
            double estimatedRows,
            int depth,
            double estimatedCost
    ) {
        this(
                nodeId,
                estimatedRows,
                depth,
                estimatedCost,
                Set.of(),
                Set.of(),
                "",
                "",
                0,
                0,
                0,
                0,
                "UNKNOWN",
                false
        );
    }
}
