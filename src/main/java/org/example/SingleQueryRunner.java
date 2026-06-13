package org.example;

import org.example.calcite.CalciteContext;
import org.example.query.QueryResult;
import org.example.query.QueryService;

/**
 * @deprecated Use {@link QueryService} via {@link Main} instead.
 */
@Deprecated
public class SingleQueryRunner {

    public static QueryResult run(
            CalciteContext ctx,
            String sql,
            boolean verbose
    ) throws Exception {
        return QueryService.run(
                ctx,
                sql,
                verbose
        );
    }
}
