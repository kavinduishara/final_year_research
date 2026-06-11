package org.example.distributed;

public record WorkerNode(
        String name,
        String jdbcUrl,
        String user,
        String password
) {
}