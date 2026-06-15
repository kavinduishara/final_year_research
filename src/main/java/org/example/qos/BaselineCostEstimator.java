package org.example.qos;

import org.example.config.ResearchSettings;
import org.example.plan.CutCandidate;

import java.util.Map;
import java.util.Set;

/**
 * Cost models used only by traditional baselines (not by RL rewards).
 *
 * <p>Each baseline mode ({@code weighted-ship}, {@code pessimistic-qos}, etc.)
 * maps the same Q1 cut candidates to a single scalar decision cost.
 *
 * <p>Example cut@38 (worker1/worker2, intermediate rows≈45000):
 * <pre>
 *   decisionCost(…, mode="weighted-ship")
 *     → intermediateTransfer + 0.1 × baseTableTransfer
 *   decisionCost(…, mode="pessimistic-qos")
 *     → baseTableTransfer + estimate(max(rows, orders×factor))
 * </pre>
 */
public final class BaselineCostEstimator {

    private BaselineCostEstimator() {
    }

    /**
     * Picks the cost formula for the configured {@link ResearchSettings#baselineMode()}.
     *
     * @param base                  cut statistics (rows, cost)
     * @param fragment1Tables       e.g. {customer, orders}
     * @param fragment1Worker       e.g. "worker1"
     * @param fragment2Worker       e.g. "worker2"
     * @param baseTableTransfer     bytes estimate for shipping base tables
     * @param intermediateTransfer  bytes estimate for intermediate result
     * @param totalTransfer         base + intermediate
     * @param tableRows             per-table row counts
     * @return scalar decision cost used by baseline policies
     */
    public static double decisionCost(
            CutCandidate base,
            Set<String> fragment1Tables,
            String fragment1Worker,
            String fragment2Worker,
            double baseTableTransfer,
            double intermediateTransfer,
            double totalTransfer,
            Map<String, Double> tableRows
    ) {
        return switch (ResearchSettings.baselineMode()) {
            case "weighted-ship" ->
                    weightedShipCost(
                            baseTableTransfer,
                            intermediateTransfer
                    );
            case "pessimistic-qos" ->
                    pessimisticTransferCost(
                            base,
                            fragment1Tables,
                            fragment1Worker,
                            fragment2Worker,
                            baseTableTransfer,
                            tableRows
                    );
            case "calcite-cost" ->
                    calciteCost(
                            base,
                            totalTransfer
                    );
            default ->
                    totalTransfer;
        };
    }

    /**
     * Models optimizers that under-weight network shipping.
     * Deep cuts look cheap because intermediate transfer is zero.
     *
     * @return e.g. intermediateTransfer + 0.1 × baseTableTransfer
     */
    private static double weightedShipCost(
            double baseTableTransfer,
            double intermediateTransfer
    ) {
        return intermediateTransfer
                + ResearchSettings.baselineShipWeight()
                        * baseTableTransfer;
    }

    /**
     * Models optimizers that ignore join selectivity and over-estimate
     * intermediate result size at shallow cuts.
     *
     * @return baseTableTransfer + pessimistic intermediate transfer estimate
     */
    private static double pessimisticTransferCost(
            CutCandidate base,
            Set<String> fragment1Tables,
            String fragment1Worker,
            String fragment2Worker,
            double baseTableTransfer,
            Map<String, Double> tableRows
    ) {
        double intermediateRows =
                pessimisticIntermediateRows(
                        base,
                        fragment1Tables,
                        tableRows
                );

        double intermediateTransfer = 0;

        if (!fragment1Worker.equals(fragment2Worker)) {
            intermediateTransfer =
                    TransferCostEstimator.estimate(
                            intermediateRows
                    );
        }

        return baseTableTransfer + intermediateTransfer;
    }

    /**
     * Inflates row estimate when customer+orders are in fragment1 (shallow cut).
     *
     * @param base              cut with metadata row count
     * @param fragment1Tables   tables below the cut
     * @param tableRows         e.g. orders → 1_500_000
     * @return pessimistic row count, e.g. max(45000, 1_500_000 × factor)
     */
    static double pessimisticIntermediateRows(
            CutCandidate base,
            Set<String> fragment1Tables,
            Map<String, Double> tableRows
    ) {
        double estimate =
                base.estimatedRows() > 0
                        ? base.estimatedRows()
                        : 0;

        if (fragment1Tables.contains("customer")
                && fragment1Tables.contains("orders")) {

            double ordersRows =
                    tableRows.getOrDefault(
                            "orders",
                            1_500_000.0
                    );

            estimate =
                    Math.max(
                            estimate,
                            ordersRows
                                    * ResearchSettings
                                    .baselinePessimismFactor()
                    );
        }

        return estimate;
    }

    /**
     * Uses Calcite self-cost when available; otherwise total transfer bytes.
     */
    private static double calciteCost(
            CutCandidate base,
            double totalTransferFallback
    ) {
        if (base.estimatedCost() >= 0) {
            return base.estimatedCost();
        }

        return totalTransferFallback;
    }
}
