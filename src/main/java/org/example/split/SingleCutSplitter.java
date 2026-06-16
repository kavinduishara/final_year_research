package org.example.split;



import org.apache.calcite.plan.RelOptUtil;

import org.apache.calcite.rel.RelNode;

import org.apache.calcite.schema.SchemaPlus;

import org.apache.calcite.tools.RelBuilder;

import org.example.experiment.ExecutionSession;

import org.example.rl.Action;



import java.util.List;



/**

 * Splits a query plan at one JOIN into two fragments + intermediate table name.

 *

 * <p>Example Q1, Action(42) at orders⋈lineitem join:

 * <pre>

 *   fragment1 = subtree below join 42 (customer+orders+lineitem joined up to that point)

 *   fragment2 = original plan with join 42 replaced by SCAN inter_single_n42

 *   placeholder = "inter_single_n42"

 * </pre>

 * Fragment1 becomes CREATE TABLE AS; fragment2 becomes the final SELECT.

 */

public class SingleCutSplitter {

 

    /**

     * Split plan at the JOIN identified by action.cutNodeId().

     *

     * @param bestPlan   full Q1 RelNode from BestPlanFinder

     * @param action     e.g. Action(42)

     * @param schema     Calcite root schema (registers IntermediateTable placeholder)

     * @param relBuilder used to build fragment1 projection and scan replacement

     * @param session    names intermediate table, e.g. "inter_single_n42"

     * @return SplitResult(fragment1, fragment2, "inter_single_n42")

     * @throws IllegalArgumentException if cutNodeId not found in plan tree

     */

    public SplitResult split(

            RelNode bestPlan,

            Action action,

            SchemaPlus schema,

            RelBuilder relBuilder,

            ExecutionSession session

    ) {



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

                session.intermediateTableName(

                        action.cutNodeId()

                );



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



    /** DFS search for RelNode with matching Calcite id. @return node or null */

    private RelNode findById(

            RelNode root,

            int id

    ) {



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


