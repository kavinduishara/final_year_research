package org.example.distributed;

import java.util.Set;

public record WorkerNode(
        String name,
        String jdbcUrl,
        String user,
        String password,
        Set<String> tables
) {
}
