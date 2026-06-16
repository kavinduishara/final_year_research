package org.example.split;

import org.apache.calcite.rel.RelNode;

/**
 * Outcome of splitting a plan at one JOIN cut.
 *
 * <p>Example Q1 cut at node 42 (worker1 has customer+orders, worker2 has lineitem):
 * <pre>
 *   fragment1  = subtree below cut (customer ⋈ orders ⋈ lineitem, up to aggregate inputs)
 *   fragment2  = remainder (aggregate over intermediate scan)
 *   placeholderName = "inter_single_n42"
 * </pre>
 * 
 * @param fragment1       RelNode executed on fragment1 worker (creates intermediate table)
 * @param fragment2       RelNode executed on fragment2 worker (reads intermediate + finishes query)
 * @param placeholderName PostgreSQL temp table name, e.g. "inter_single_n42"
 */
public record SplitResult(RelNode fragment1, RelNode fragment2, String placeholderName) {}
