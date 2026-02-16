package org.example.split;

import org.apache.calcite.rel.RelNode;

public record SplitResult(RelNode fragment1, RelNode fragment2, String placeholderName) {}
