package org.example.experiment;

import org.apache.calcite.rel.RelNode;
import org.example.calcite.CalciteContext;
import org.example.exec.ExecutionMetrics;
import org.example.plan.CutCandidate;
import org.example.rl.Action;

import java.util.List;

/**
 * Thin wrapper: executes one chosen cut for a single query experiment.
 *
 * <p>Example Q1 with learned action cut@38:
 * <pre>
 *   run(bestPlan, Action(38), ctx, candidates)
 *   → DistributedQueryRunner on worker1/worker2
 *   → inter_single_n38 materialized, fragment2 completes
 *   → returns ExecutionMetrics(totalTimeMs=…)
 * </pre>
 */
public class ExperimentRunner {

    /**
     * Dispatches to {@link DistributedQueryRunner} for the selected cut.
     *
     * @param bestPlan   optimized Q1 RelNode root
     * @param action     chosen cut node id, e.g. 38 or 42
     * @param ctx        Calcite planning context
     * @param candidates enriched cuts (must include action's nodeId)
     * @return execution metrics for the run
     * @throws Exception on distributed execution failure
     */
    public static ExecutionMetrics run(
            RelNode bestPlan,
            Action action,
            CalciteContext ctx,
            List<CutCandidate> candidates
    ) throws Exception {

        CutCandidate chosen =
                candidates.stream()
                        .filter(c ->
                                c.nodeId()
                                        == action.cutNodeId()
                        )
                        .findFirst()
                        .orElseThrow();

        return DistributedQueryRunner.execute(
                bestPlan,
                action,
                ctx,
                chosen,
                ExecutionSession.single(),
                true
        ).metrics();
    }
}
