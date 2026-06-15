package org.example.query;

import org.example.exec.ExecutionMetrics;
import org.example.rl.CutSelection;

import java.util.List;

/**
 * Final output of a query run (inference mode returns rows; train mode often has empty rows).
 *
 * <p>Example (inference on Q1, cut node 42 chosen):
 * <pre>
 *   sql              = "SELECT c.mktsegment, SUM(o.totalprice) FROM customer c JOIN ..."
 *   chosenCutNodeId  = 42
 *   selectionReason  = LINUCB
 *   bestScore        = 0.73
 *   columnNames      = ["mktsegment", "EXPR$1"]
 *   rows             = [["BUILDING", 1234567.89], ["AUTOMOBILE", ...], ...]
 *   metrics          = ExecutionMetrics(f1=1200ms, ship=800ms, transfer=500ms, f2=900ms)
 * </pre>
 */
public record QueryResult(
        /** Original SQL string that was executed. */
        String sql,
        /** Calcite RelNode id of the chosen JOIN cut (e.g. 42). */
        int chosenCutNodeId,
        /** Why this cut was picked: LINUCB, EXECUTION_BEST, FALLBACK_CHEAPEST_TRANSFER, … */
        CutSelection.SelectionReason selectionReason,
        /** Bandit score or Q-value (0.0 if fallback). */
        double bestScore,
        /** Column names from fragment2 SELECT (empty in train mode). */
        List<String> columnNames,
        /** Result rows as List of column values (empty in train mode). */
        List<List<Object>> rows,
        /** Timing breakdown from distributed execution (null only in edge cases). */
        ExecutionMetrics metrics
) {

    /** Prints cut choice, row count, and timing summary to stdout. */
    public void printSummary() {

        System.out.println(
                "\n===== QUERY RESULT ====="
        );

        System.out.println(
                "Chosen cut node = "
                        + chosenCutNodeId
        );

        System.out.println(
                "Selection       = "
                        + selectionReason
        );

        if (selectionReason
                == CutSelection.SelectionReason.LINUCB
                || selectionReason
                == CutSelection.SelectionReason.EXECUTION_BEST) {
            System.out.println(
                    "Best score      = "
                            + bestScore
            );
        }

        System.out.println(
                "Rows returned   = "
                        + rows.size()
        );

        if (metrics != null) {
            System.out.println(
                    "Total time      = "
                            + metrics.totalTimeMs()
                            + " ms"
            );

            System.out.println(
                    "Runtime (F1+F2) = "
                            + metrics.runtimeMs()
                            + " ms"
            );

            System.out.println(
                    "Transfer        = "
                            + metrics.transferTimeMs()
                            + " ms"
            );
        }
    }

    /** Prints up to 50 result rows to stdout. */
    public void printResults() {
        printResults(50);
    }

    /**
     * Prints column header and up to maxRows data rows.
     *
     * @param maxRows maximum rows to display (e.g. 50)
     */
    public void printResults(int maxRows) {

        if (columnNames.isEmpty()) {
            System.out.println(
                    "\n(no result rows — train mode uses cached metrics only; "
                            + "use inference mode for full results)"
            );
            return;
        }

        System.out.println(
                "\n===== RESULT ROWS ====="
        );

        System.out.println(
                String.join(" | ", columnNames)
        );

        System.out.println(
                "-".repeat(60)
        );

        int limit =
                Math.min(
                        maxRows,
                        rows.size()
                );

        for (int i = 0; i < limit; i++) {

            List<Object> row =
                    rows.get(i);

            StringBuilder line =
                    new StringBuilder();

            for (int c = 0; c < row.size(); c++) {

                line.append(
                        row.get(c)
                );

                if (c < row.size() - 1) {
                    line.append(" | ");
                }
            }

            System.out.println(line);
        }

        if (rows.size() > limit) {
            System.out.println(
                    "... ("
                            + (rows.size() - limit)
                            + " more rows)"
            );
        }
    }
}
