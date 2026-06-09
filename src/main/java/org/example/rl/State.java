package org.example.rl;

public record State(
        String rowBucket,
        String depthBucket,
        String costBucket
) {
}