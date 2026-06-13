package org.example.distributed;

import org.example.exec.ExecutionMetrics;

import java.util.List;

public record DistributedExecutionResult(
        ExecutionMetrics metrics,
        List<String> columnNames,
        List<List<Object>> rows
) {
}
