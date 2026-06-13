package org.example.query;

import org.example.experiment.ResearchQueryWorkload;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SqlInputParser {

    public record CliOptions(
            String sql,
            boolean verbose,
            boolean help,
            boolean sqlFromCli
    ) {
    }

    private SqlInputParser() {
    }

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
                  training.mode=train            Train one query, save qtable.json
                  training.mode=workload-train   Train all 10 workload queries into one qtable.json
                  training.mode=inference        Load qtable, run one cut, return results
                  training.mode=benchmark        Run 10 built-in queries (compare baseline vs RL)

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
