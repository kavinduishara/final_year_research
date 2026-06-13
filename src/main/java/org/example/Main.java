package org.example;

import org.example.calcite.CalciteContext;
import org.example.calcite.CalcitePlannerFactory;
import org.example.config.ResearchSettings;
import org.example.experiment.QueryBenchmarkRunner;

public class Main {

    public static void main(String[] args) throws Exception {

        CalciteContext ctx =
                CalcitePlannerFactory.createFromMetaDb();

        if (ResearchSettings.isBenchmarkMode()) {
            QueryBenchmarkRunner.run(ctx);
            return;
        }

        SingleQueryRunner.run(ctx);
    }
}
