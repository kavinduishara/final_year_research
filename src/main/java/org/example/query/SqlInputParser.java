package org.example.query;

import org.example.experiment.ResearchQueryWorkload;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Parses command-line arguments into SQL text and flags.
 *
 * <p>Example:
 * <pre>
 *   parse(new String[]{"--verbose"})
 *   → CliOptions(sql=Q1 default SQL, verbose=true, help=false, sqlFromCli=false)
 *
 *   parse(new String[]{"--sql", "SELECT 1"})
 *   → CliOptions(sql="SELECT 1", verbose=false, help=false, sqlFromCli=true)
 * </pre>
 */
public final class SqlInputParser {

    /**
     * Parsed CLI result.
     *
     * @param sql          final SQL to run (never null unless help=true)
     * @param verbose      if true, print plan/cut details in QueryService
     * @param help         if true, caller should print usage and exit
     * @param sqlFromCli   true if user passed --sql or --file (ignored in benchmark mode)
     */
    public record CliOptions(
            String sql,
            boolean verbose,
            boolean help,
            boolean sqlFromCli
    ) {
    }

    private SqlInputParser() {
    }

    /**
     * Walks args and builds CliOptions.
     *
     * @param args command-line tokens, e.g. {@code ["--file", "q1.sql", "-v"]}
     * @return CliOptions; if no SQL given, uses ResearchQueryWorkload.defaultSql() (Q1)
     * @throws IOException if --file path cannot be read
     */
    public static CliOptions parse(
            String[] args
    ) throws IOException {

        String sql = null;
        boolean verbose = false;
        boolean sqlFromCli = false;

        for (int i = 0; i < args.length; i++) {

            String arg = args[i];

            if ("--help".equals(arg)
                    || "-h".equals(arg)) {
                return new CliOptions(
                        null,
                        false,
                        true,
                        false
                );
            }

            if ("--verbose".equals(arg)
                    || "-v".equals(arg)) {
                verbose = true;
                continue;
            }

            if ("--sql".equals(arg)
                    && i + 1 < args.length) {
                sql = args[++i];
                sqlFromCli = true;
                continue;
            }

            if ("--file".equals(arg)
                    && i + 1 < args.length) {
                sql =
                        Files.readString(
                                Path.of(args[++i])
                        ).trim();
                sqlFromCli = true;
            }
        }

        if (sql == null) {
            // Default: Q1_mktsegment_revenue (customer JOIN orders JOIN lineitem)
            sql =
                    ResearchQueryWorkload
                            .defaultSql();
        }
        else {
            sql =
                    org.example.sql.SqlNormalizer.forCalcite(
                            sql
                    );
        }

        return new CliOptions(
                sql,
                verbose,
                false,
                sqlFromCli
        );
    }

    /** Prints CLI help text to stdout. Returns nothing. */
    public static void printUsage() {

        System.out.println(
                """
                Usage:
                  java -jar query_processer.jar [options]

                Options:
                  --sql "SELECT ..."   SQL query string
                  --file path.sql      SQL from file
                  --verbose, -v        Print plan and cut details
                  --help, -h           Show this help

                Modes (application.properties):
                  training.mode=train            Train one query, save bandit.json
                  training.mode=workload-train   Train all 10 workload queries
                  training.mode=inference        Load bandit.json, pick best cut
                  training.mode=benchmark        Compare baseline vs LinUCB

                Examples:
                  training.mode=train
                  java -jar query_processer.jar --sql "SELECT ..."

                  training.mode=workload-train
                  java -jar query_processer.jar

                  training.mode=inference
                  java -jar query_processer.jar --sql "SELECT ..."
                """
        );
    }
}
