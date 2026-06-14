package org.example.query;

import org.example.exec.ExecutionMetrics;
import org.example.rl.CutSelection;

import java.util.List;

public record QueryResult(
        String sql,
        int chosenCutNodeId,
        CutSelection.SelectionReason selectionReason,
        double bestScore,
        List<String> columnNames,
        List<List<Object>> rows,
        ExecutionMetrics metrics
) {

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

    public void printResults() {
        printResults(50);
    }

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
