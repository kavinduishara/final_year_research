package org.example.plan;

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.metadata.RelMetadataQuery;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds initial {@link CutCandidate} records from JOIN cut points.
 *
 * <p>Captures planner statistics (row count, depth, self-cost) before locality
 * enrichment. Does not yet know about workers or transfer costs.
 *
 * <p>Example Q1 with cuts [Join#38, Join#42]:
 * <pre>
 *   collect(plan, cutPoints) → [
 *     CutCandidate(nodeId=38, estimatedRows≈45000, depth=2, cost=…),
 *     CutCandidate(nodeId=42, estimatedRows≈12000, depth=1, cost=…)
 *   ]
 * </pre>
 */
public class PlanStatisticsCollector {

    /**
     * For each cut RelNode, reads Calcite metadata and wraps a base candidate.
     *
     * @param root      best plan (Q1 aggregate root)
     * @param cutPoints JOIN nodes from {@link CutPointCollector}, e.g. [Join#38, Join#42]
     * @return base candidates (locality fields unset until {@link FragmentLocalityAnalyzer})
     */
    public static List<CutCandidate> collect(RelNode root,
                                             List<RelNode> cutPoints){

        List<CutCandidate> candidates = new ArrayList<>();

        for (RelNode node : cutPoints) {
            int depth =
                    RelNodeDepthCalculator.depth(
                            root,
                            node
                    );

            RelMetadataQuery mq =
                    node.getCluster().getMetadataQuery();

            Double rowCount =
                    mq.getRowCount(node);
            double cost = -1;

            try {
                cost = node.computeSelfCost(
                        node.getCluster().getPlanner(),
                        mq
                ).getRows();
            }
            catch (Exception e) {
                System.out.println(
                        "Unable to estimate cost for node "
                                + node.getId()
                );
            }

            candidates.add(
                    new CutCandidate(
                            node.getId(),
                            rowCount == null ? -1 : rowCount,
                            depth,
                            cost
                    )
            );
        }

        return candidates;
    }
}