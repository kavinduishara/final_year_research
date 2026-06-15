package org.example.sql;

/**
 * Executable SQL pair for a two-phase distributed cut.
 *
 * <p>Example Q1 cut at node 42, session "single":
 * <pre>
 *   tempTableName = "inter_single_n42"
 *   dropSql       = DROP TABLE IF EXISTS "inter_single_n42" CASCADE
 *   createSql     = CREATE TABLE "inter_single_n42" AS SELECT … (fragment1)
 *   sql2          = SELECT c.mktsegment, SUM(o.totalprice) … FROM "inter_single_n42" …
 *   sql1()        = dropSql + createSql (run on worker1)
 * </pre>
 *
 * @param dropSql       idempotent drop before recreate
 * @param createSql     CTAS materializing fragment1 output
 * @param sql2          final query reading the intermediate table on worker2
 * @param tempTableName unquoted name, e.g. "inter_single_n42"
 */
public record FragmentSql(
        String dropSql,
        String createSql,
        String sql2,
        String tempTableName
) {
    /**
     * Combined phase-1 script (drop then create).
     *
     * @return multi-statement SQL for fragment1 worker, e.g. DROP …; CREATE TABLE … AS SELECT …
     */
    public String sql1() {
        return dropSql
                + "\n"
                + createSql;
    }
}
