package org.example.distributed;

import java.util.List;

/**
 * In-memory tabular result from a worker JDBC query.
 *
 * <p>Example Q1 fragment2 on worker2:
 * <pre>
 *   columnNames = ["mktsegment", "sum"]
 *   rows        = [["BUILDING", 1.23e8], ["AUTOMOBILE", 9.87e7], …]
 * </pre>
 *
 * @param columnNames result set column labels from JDBC metadata
 * @param rows        one {@link List} per row; cell values as returned by {@code ResultSet#getObject}
 */
public record CollectedRows(
        List<String> columnNames,
        List<List<Object>> rows
) {
}
