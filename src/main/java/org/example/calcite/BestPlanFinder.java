package org.example.calcite;

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelRoot;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.tools.Planner;

public class BestPlanFinder {

    /**
     * V0: parse → validate → toRel
     * This yields a logical plan (RelNode tree). That is enough for splitting research.
     * Later you can add real optimization programs / rule sets if needed.
     */
    public static RelNode sqlToRel(String sql, CalciteContext ctx) throws Exception {
        Planner planner = ctx.planner();

        SqlNode parsed = planner.parse(sql);
        SqlNode validated = planner.validate(parsed);
        RelRoot root = planner.rel(validated);

        return root.rel;
    }
}
