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

public class FragmentLocalityAnalyzer {

    public static CutCandidate enrich(
            RelNode root,
            RelNode cutNode,
            CutCandidate base,
            TableDistribution distribution,
            List<WorkerNode> workers,
            boolean shippingEnabled
    ) {
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
