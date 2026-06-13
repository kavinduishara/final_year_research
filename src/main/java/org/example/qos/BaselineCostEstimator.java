package org.example.qos;

import org.example.config.ResearchSettings;
import org.example.plan.CutCandidate;

import java.util.Map;
import java.util.Set;

/**
 * Cost models used only by traditional baselines (not by RL rewards).
 */
public final class BaselineCostEstimator {

    private BaselineCostEstimator() {
    }

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
