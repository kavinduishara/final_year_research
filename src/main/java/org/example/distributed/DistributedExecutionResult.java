package org.example.distributed;

import org.example.exec.ExecutionMetrics;

import java.util.List;

/**
 * End-to-end outcome of a distributed Q1-style execution.
 *
 * <p>Combines timing breakdown ({@link ExecutionMetrics}) with the final
 * result rows from fragment2 (e.g. mktsegment × SUM(totalprice) on worker2).
 *
 * @param metrics     runtime, transfer, and total times for the chosen cut
 * @param columnNames final query column labels
 * @param rows        aggregated result rows returned to the caller
 */
public record DistributedExecutionResult(
        ExecutionMetrics metrics,
        List<String> columnNames,
        List<List<Object>> rows
) {
}
