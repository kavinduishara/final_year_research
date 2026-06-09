package org.example.plan;

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.metadata.RelMetadataQuery;

import java.util.ArrayList;
import java.util.List;

public class PlanStatisticsCollector {

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