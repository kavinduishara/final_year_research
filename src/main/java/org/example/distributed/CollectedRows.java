package org.example.distributed;

import java.util.List;

public record CollectedRows(
        List<String> columnNames,
        List<List<Object>> rows
) {
}
