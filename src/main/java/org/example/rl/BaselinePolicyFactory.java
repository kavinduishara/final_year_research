package org.example.rl;

import org.apache.calcite.rel.RelNode;
import org.example.config.ResearchSettings;
import org.example.plan.CutCandidate;

import java.util.Comparator;
import java.util.List;

public final class BaselinePolicyFactory {

    private BaselinePolicyFactory() {
    }

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
