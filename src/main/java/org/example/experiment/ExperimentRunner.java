package org.example.experiment;

import org.apache.calcite.rel.RelNode;
import org.example.calcite.CalciteContext;
import org.example.exec.ExecutionMetrics;
import org.example.plan.CutCandidate;
import org.example.rl.Action;

import java.util.List;

public class ExperimentRunner {

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
