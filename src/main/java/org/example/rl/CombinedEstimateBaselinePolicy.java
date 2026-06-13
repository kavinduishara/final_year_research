package org.example.rl;

import org.example.config.ResearchSettings;
import org.example.plan.CutCandidate;

import java.util.Comparator;
import java.util.List;

/**
 * Combined estimate baseline: transfer cost plus Calcite plan cost.
 * Models optimizers that balance I/O/network estimates with planner cost.
 */
public class CombinedEstimateBaselinePolicy implements CutPolicy {

    @Override
    public Action choose(
            List<CutCandidate> candidates
    ) {
        double transferWeight =
                ResearchSettings.baselineTransferWeight();

        CutCandidate best =
                candidates.stream()
                        .filter(CutCandidate::executable)
                        .min(Comparator
                                .<CutCandidate>comparingDouble(
                                        candidate ->
                                                combinedScore(
                                                        candidate,
                                                        transferWeight
                                                )
                                )
                                .thenComparingInt(
                                        CutCandidate::nodeId
                                ))
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "No executable cut for combined baseline"
                                ));

        return new Action(
                best.nodeId()
        );
    }

    private static double combinedScore(
            CutCandidate candidate,
            double transferWeight
    ) {
        return candidate.estimatedCost()
                + transferWeight
                        * candidate.totalTransferCost();
    }
}
