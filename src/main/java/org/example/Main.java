package org.example;

import org.example.calcite.CalciteContext;
import org.example.calcite.CalcitePlannerFactory;
import org.example.config.ResearchSettings;
import org.example.experiment.QueryBenchmarkRunner;
import org.example.query.QueryService;
import org.example.query.SqlInputParser;

public class Main {

    public static void main(String[] args) throws Exception {

        SqlInputParser.CliOptions options =
                SqlInputParser.parse(args);

        if (options.help()) {
            SqlInputParser.printUsage();
            return;
        }

        CalciteContext ctx =
                CalcitePlannerFactory.createFromMetaDb();

        if (ResearchSettings.isBenchmarkMode()) {
            if (options.sqlFromCli()) {
                System.out.println(
                        "Note: benchmark mode runs the 10 built-in "
                                + "ResearchQueryWorkload queries; "
                                + "--sql / --file is ignored."
                );
                System.out.println(
                        "For a custom query use training.mode=train "
                                + "or training.mode=inference."
                );
                System.out.println();
            }
            QueryBenchmarkRunner.run(ctx);
            return;
        }

        QueryService.run(
                ctx,
                options.sql(),
                options.verbose()
        );
    }
}
