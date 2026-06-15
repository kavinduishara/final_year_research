package org.example.rl;

import org.apache.calcite.rel.RelNode;
import org.example.config.ResearchSettings;
import org.example.plan.CutCandidate;

import java.util.Comparator;
import java.util.List;

/**
 * Factory for static baseline cut-selection policies (research comparators).
 *
 * <p>Baseline mode is configured via {@code research.baseline-mode} in settings.
 * Q1 example with worker1/worker2:
 * <pre>
 *   "static-qos"       → lowest totalTransferCost (may pick cut@38)
 *   "weighted-ship"    → under-weights base shipping (often picks cut@42)
 *   "deep-join"        → deepest executable JOIN (cut@38 if deeper)
 *   "pessimistic-qos"  → inflates shallow-cut intermediate size
 * </pre>
 */
public final class BaselinePolicyFactory {

    private BaselinePolicyFactory() {
    }

    /**
     * Dispatches to the configured baseline implementation.
     *
     * @param candidates enriched Q1 cuts [38, 42]
     * @param cutPoints  raw JOIN RelNodes (needed for deep-join mode)
     * @return chosen cut action, e.g. Action(42)
     */
    public static Action choose(
            List<CutCandidate> candidates,
            List<RelNode> cutPoints
    ) {
        return switch (baselineMode()) {
            case "deep-join" ->
                    deepJoin(candidates, cutPoints);
            case "combined-estimate" ->
                    new CombinedEstimateBaselinePolicy()
                            .choose(candidates);
            case "calcite-cost" ->
                    new CalciteCostBaselinePolicy()
                            .choose(candidates);
            case "pessimistic-qos" ->
                    new PessimisticQoSBaselinePolicy()
                            .choose(candidates);
            case "weighted-ship" ->
                    new WeightedShipBaselinePolicy()
                            .choose(candidates);
            default ->
                    new StaticQoSBaselinePolicy()
                            .choose(candidates);
        };
    }

    /**
     * @return short label for benchmark output, e.g. "WeightedShip"
     */
    public static String displayName() {
        return switch (baselineMode()) {
            case "deep-join" -> "DeepJoin";
            case "combined-estimate" -> "CombinedEstimate";
            case "calcite-cost" -> "CalciteCost";
            case "pessimistic-qos" -> "PessimisticQoS";
            case "weighted-ship" -> "WeightedShip";
            default -> "StaticQoS";
        };
    }

    private static String baselineMode() {
        return ResearchSettings.baselineMode();
    }

    /**
     * Picks the executable cut with maximum depth (deepest JOIN in the tree).
     *
     * @param candidates statistics + locality
     * @param cutPoints  JOIN nodes from plan
     * @return action for deepest cut, e.g. Action(38) when depth(38) &gt; depth(42)
     */
    private static Action deepJoin(
            List<CutCandidate> candidates,
            List<RelNode> cutPoints
    ) {
        if (cutPoints == null || cutPoints.isEmpty()) {
            throw new IllegalArgumentException(
                    "No legal cut points."
            );
        }

        CutCandidate deepest =
                candidates.stream()
                        .filter(CutCandidate::executable)
                        .max(Comparator
                                .comparingInt(
                                        CutCandidate::depth
                                )
                                .thenComparingInt(
                                        CutCandidate::nodeId
                                ))
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "No executable cut for deep-join baseline"
                                ));

        return new Action(
                deepest.nodeId()
        );
    }
}
