package org.example.rl;

/**
 * RL action: which JOIN node in the Calcite plan to cut.
 *
 * <p>Example: Q1 plan has JOIN nodes with ids 38, 42, 45.
 * Action(42) means "cut at the orders⋈lineitem join (node id 42)".
 */
public record Action(int cutNodeId) {
    @Override
    public String toString() {
        return "Action{cutNodeId=" + cutNodeId + "}";
    }
}
