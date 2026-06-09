package org.example.plan;

import org.apache.calcite.rel.RelNode;

public class RelNodeDepthCalculator {

    public static int depth(RelNode root, RelNode target) {
        return depth(root, target, 0);
    }

    private static int depth(RelNode current,
                             RelNode target,
                             int currentDepth) {

        if (current == target) {
            return currentDepth;
        }

        for (RelNode input : current.getInputs()) {
            int found =
                    depth(input,
                            target,
                            currentDepth + 1);

            if (found != -1) {
                return found;
            }
        }

        return -1;
    }
}