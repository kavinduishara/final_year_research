package org.example;

import org.example.calcite.CalciteContext;
import org.example.calcite.CalcitePlannerFactory;
import org.example.config.ResearchSettings;
import org.example.experiment.QueryBenchmarkRunner;
import org.example.experiment.WorkloadTrainer;
import org.example.query.QueryService;
import org.example.query.SqlInputParser;

/**
 * Application entry point.
 *
 * <p>Example run (inference on Q1):
 * <pre>
 *   training.mode=inference   (in application.properties)
 *   java -jar query_processer.jar --verbose
 * </pre>
 * Q1 joins customer + orders + lineitem and groups by mktsegment.
 * Main connects to meta DB (5432), then either benchmarks 10 queries,
 * trains on all 10, or runs a single query via QueryService.
 */
public class Main {

    /**
     * Parses CLI args, builds Calcite context from meta DB, dispatches by training.mode.
     *
     * @param args e.g. {@code ["--sql", "SELECT ...", "--verbose"]} or {@code []} for default Q1
     * @throws Exception if DB connection or query planning fails
     *
     * <p>Mode routing (from application.properties training.mode):
     * <ul>
     *   <li>{@code benchmark}      → QueryBenchmarkRunner.run(ctx) — 10 queries, baseline vs LinUCB</li>
     *   <li>{@code workload-train} → WorkloadTrainer.run(ctx) — train shared bandit on all 10</li>
     *   <li>{@code train/inference}→ QueryService.run(ctx, sql, verbose) — one query</li>
     * </ul>
     */
    public static void main(String[] args) throws Exception {

        // Example: args = ["--verbose"] → sql defaults to Q1, verbose=true
        SqlInputParser.CliOptions options =
                SqlInputParser.parse(args);

        if (options.help()) {
            SqlInputParser.printUsage();
            return;
        }

        // Connects to jdbc:postgresql://localhost:5432/public, loads all schemas into Calcite
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
            // Returns List<QueryBenchmarkRow> internally; prints summary table
            QueryBenchmarkRunner.run(ctx);
            return;
        }

        if (ResearchSettings.isWorkloadTrainMode()) {
            if (options.sqlFromCli()) {
                System.out.println(
                        "Note: workload-train mode runs all "
                                + "ResearchQueryWorkload queries; "
                                + "--sql / --file is ignored."
                );
                System.out.println();
            }
            // Trains one shared LinUCB model across Q1..Q10, saves bandit.json
            WorkloadTrainer.run(ctx);
            return;
        }

        // Single-query path: train (execute all cuts) or inference (pick best cut, run once)
        QueryService.run(
                ctx,
                options.sql(),
                options.verbose()
        );
    }
}
