package org.example.split;

import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.tools.RelBuilder;
import org.example.rl.Action;

import java.util.List;

public class SingleCutSplitter {

    public SplitResult split(
            RelNode bestPlan,
            Action action,
            SchemaPlus schema,
            RelBuilder relBuilder) {

        RelNode cut =
                findById(
                        bestPlan,
                        action.cutNodeId()
                );

        if (cut == null) {
            throw new IllegalArgumentException(
                    "cutNodeId not found: "
                            + action.cutNodeId()
            );
        }

        String tmpName =
                "INTERMEDIATE_"
                        + action.cutNodeId();

        if (schema.getTable(tmpName) == null) {
            schema.add(
                    tmpName,
                    new IntermediateTable(
                            cut.getRowType()
                    )
            );
        }

        List<String> fields =
                cut.getRowType()
                        .getFieldNames();

        RelNode fragment1 =
                relBuilder
                        .push(cut)
                        .project(
                                fields.stream()
                                        .map(name ->
                                                relBuilder.field(name)
                                        )
                                        .toList(),
                                fields
                        )
                        .build();

        System.out.println(
                "\n===== FRAGMENT1 AFTER PROJECT ====="
        );

        System.out.println(
                RelOptUtil.toString(
                        fragment1
                )
        );

        RelNode replacement =
                relBuilder
                        .scan(tmpName)
                        .build();

        RelNode fragment2 =
                RelTreeRewriter.replace(
                        bestPlan,
                        cut,
                        replacement
                );

        return new SplitResult(
                fragment1,
                fragment2,
                tmpName
        );
    }

    private RelNode findById(
            RelNode root,
            int id) {

        if (root.getId() == id) {
            return root;
        }

        for (RelNode in : root.getInputs()) {

            RelNode found =
                    findById(
                            in,
                            id
                    );

            if (found != null) {
                return found;
            }
        }

        return null;
    }
}