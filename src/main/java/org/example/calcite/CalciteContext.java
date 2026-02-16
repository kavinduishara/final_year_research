package org.example.calcite;

import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.tools.FrameworkConfig;
import org.apache.calcite.tools.Planner;
import org.apache.calcite.tools.RelBuilder;

public record CalciteContext(SchemaPlus rootSchema, Planner planner, FrameworkConfig config, RelBuilder relBuilder) {}
