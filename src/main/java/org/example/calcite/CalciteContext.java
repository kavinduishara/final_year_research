package org.example.calcite;

import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.tools.FrameworkConfig;
import org.apache.calcite.tools.RelBuilder;

/**
 * Holds everything needed to plan SQL with Apache Calcite.
 *
 * <p>Created once by {@link CalcitePlannerFactory#createFromMetaDb()} and passed
 * through the pipeline (QueryService → DistributedQueryRunner → SingleCutSplitter).
 *
 * <p>Example after factory call:
 * <ul>
 *   <li>{@code rootSchema}     — all DB schemas (public, etc.)</li>
 *   <li>{@code defaultSchema}  — "public" (tables: customer, orders, lineitem)</li>
 *   <li>{@code relBuilder}     — used to build fragment1 projection in SingleCutSplitter</li>
 * </ul>
 */
public record CalciteContext(
        /** Top-level Calcite schema tree (contains public, information_schema, …). */
        SchemaPlus rootSchema,
        /** Default schema for unqualified table names (usually "public"). */
        SchemaPlus defaultSchema,
        /** Parser + validator + planner configuration. */
        FrameworkConfig frameworkConfig,
        /** Helper to construct RelNode trees (project, scan, etc.). */
        RelBuilder relBuilder
) {
}
