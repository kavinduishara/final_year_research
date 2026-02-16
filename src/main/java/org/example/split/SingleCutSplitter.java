package org.example.split;

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.tools.RelBuilder;
import org.example.rl.Action;

public class SingleCutSplitter {

    public SplitResult split(RelNode bestPlan, Action action, SchemaPlus schema, RelBuilder relBuilder) {
        RelNode cut = findById(bestPlan, action.cutNodeId());
        if (cut == null) throw new IllegalArgumentException("cutNodeId not found: " + action.cutNodeId());

        String tmpName = "INTERMEDIATE_" + action.cutNodeId();

        // Register intermediate as a schema table (so converter will generate FROM INTERMEDIATE_x)
        if (schema.getTable(tmpName) == null) {
            schema.add(tmpName, new IntermediateTable(cut.getRowType()));
        }

        RelNode fragment1 = cut;

        // Replace subtree with a real table scan
        RelNode replacement = relBuilder.scan(tmpName).build();
        RelNode fragment2 = RelTreeRewriter.replace(bestPlan, cut, replacement);


        return new SplitResult(fragment1, fragment2, tmpName);
    }

    private RelNode findById(RelNode root, int id) {
        if (root.getId() == id) return root;
        for (RelNode in : root.getInputs()) {
            RelNode found = findById(in, id);
            if (found != null) return found;
        }
        return null;
    }
}
