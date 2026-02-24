package org.example.calcite;

import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.tools.FrameworkConfig;
import org.apache.calcite.tools.RelBuilder;

public record CalciteContext(
        SchemaPlus rootSchema,
        SchemaPlus defaultSchema,
        FrameworkConfig frameworkConfig,
        RelBuilder relBuilder
) {}
