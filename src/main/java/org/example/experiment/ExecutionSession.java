package org.example.experiment;

/**
 * Names intermediate tables per query/session so parallel runs do not collide.
 *
 * <p>Intermediate table naming convention:
 * <pre>
 *   ExecutionSession.single().intermediateTableName(42)  → "inter_single_n42"
 *   ExecutionSession.forQuery(1).intermediateTableName(42) → "inter_q1_n42"
 * </pre>
 *
 * @param id session prefix, e.g. "single" or "q1"
 */
public record ExecutionSession(String id) {

    /**
     * @param queryNumber 1-based query index in {@link ResearchQueryWorkload}
     * @return session id "q1", "q2", …
     */
    public static ExecutionSession forQuery(int queryNumber) {
        return new ExecutionSession(
                "q" + queryNumber
        );
    }

    /** Default one-off run (not part of multi-query benchmark). */
    public static ExecutionSession single() {
        return new ExecutionSession(
                "single"
        );
    }

    /**
     * PostgreSQL table name for materializing fragment1 at a cut.
     *
     * @param cutNodeId Calcite RelNode id, e.g. 38 or 42
     * @return e.g. "inter_single_n42"
     */
    public String intermediateTableName(int cutNodeId) {
        return "inter_"
                + id
                + "_n"
                + cutNodeId;
    }
}
