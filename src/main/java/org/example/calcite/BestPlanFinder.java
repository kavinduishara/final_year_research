package org.example.calcite;

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelRoot;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.tools.Frameworks;
import org.apache.calcite.tools.Planner;
import org.example.sql.SqlNormalizer;

public class BestPlanFinder {

    public static RelNode sqlToBestRel(String sql, CalciteContext ctx) throws Exception {
        Planner planner = Frameworks.getPlanner(ctx.frameworkConfig());

        String normalized =
                SqlNormalizer.forCalcite(sql);

        SqlNode parsed = planner.parse(normalized);
        SqlNode validated = planner.validate(parsed);
        RelRoot root = planner.rel(validated);

        return root.rel; // logical best plan
    }
}
