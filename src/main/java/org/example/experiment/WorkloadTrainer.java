package org.example.experiment;

import org.apache.calcite.rel.RelNode;
import org.example.calcite.BestPlanFinder;
import org.example.calcite.CalciteContext;
import org.example.config.ResearchSettings;
import org.example.distributed.ResearchEnvironmentCleaner;
import org.example.distributed.TableDistribution;
import org.example.distributed.WorkerRegistry;
import org.example.plan.CutCandidate;
import org.example.plan.CutPointCollector;
import org.example.plan.FragmentLocalityAnalyzer;
import org.example.plan.PlanStatisticsCollector;
import org.example.rl.ExecutionTrainer;
import org.example.rl.QTable;
import org.example.rl.QTableStore;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class WorkloadTrainer {

    public record WorkloadTrainRow(
            int queryNumber,
            String queryName,
            int executableCuts,
            int qTableEntriesAfter
    ) {
    }

    private WorkloadTrainer() {
    }

    public static List<WorkloadTrainRow> run(
            CalciteContext ctx
    ) throws Exception {

        TableDistribution distribution =
                WorkerRegistry.tableDistribution();

        List<ResearchQueryWorkload.ResearchQuery> queries =
                ResearchQueryWorkload.all();

        QTable qTable =
                loadOrCreateQTable();

        List<WorkloadTrainRow> rows =
                new ArrayList<>();

        System.out.println(
                "\n===== MULTI-QUERY RL TRAINING ====="
        );

        System.out.println(
                "Queries            = "
                        + queries.size()
        );

        System.out.println(
                "Episodes per query = "
                        + ResearchSettings.trainingEpisodes()
        );

        System.out.println(
                "Q-table path       = "
                        + ResearchSettings.qTablePath()
        );

        System.out.println(
                "Resume existing    = "
                        + ResearchSettings.resumeQTable()
        );

        System.out.println(
                "Starting entries   = "
                        + qTable.entries().size()
        );

        System.out.flush();

        for (int i = 0; i < queries.size(); i++) {

            ResearchQueryWorkload.ResearchQuery query =
                    queries.get(i);

            int queryNumber = i + 1;

            System.out.println(
                    "\n"
                            + "=".repeat(60)
            );

            System.out.println(
                    "TRAIN QUERY "
                            + queryNumber
                            + " / "
                            + queries.size()
                            + ": "
                            + query.name()
            );

            System.out.println(
                    "=".repeat(60)
            );

            System.out.println(query.sql());
            System.out.flush();

            ResearchEnvironmentCleaner.cleanupBetweenQueries();

            RelNode plan =
                    BestPlanFinder.sqlToBestRel(
                            query.sql(),
                            ctx
                    );

            List<RelNode> cutPoints =
                    CutPointCollector.collectJoinCuts(
                            plan
                    );

            if (cutPoints.isEmpty()) {
                System.out.println(
                        "Skipped: no JOIN cut points."
                );
                continue;
            }

            List<CutCandidate> candidates =
                    PlanStatisticsCollector.collect(
                            plan,
                            cutPoints
                    );

            candidates =
                    FragmentLocalityAnalyzer.enrichAll(
                            plan,
                            candidates,
                            distribution,
                            WorkerRegistry.workers()
                    );

            long executable =
                    candidates.stream()
                            .filter(CutCandidate::executable)
                            .count();

            System.out.println(
                    "Executable cuts = "
                            + executable
                            + " / "
                            + candidates.size()
            );

            if (executable == 0) {
                System.out.println(
                        "Skipped: no executable cuts."
                );
                continue;
            }

            ExecutionSession session =
                    ExecutionSession.forQuery(
                            queryNumber
                    );

            ExecutionTrainer.train(
                    plan,
                    ctx,
                    candidates,
                    session,
                    qTable,
                    false
            );

            WorkloadTrainRow row =
                    new WorkloadTrainRow(
                            queryNumber,
                            query.name(),
                            (int) executable,
                            qTable.entries().size()
                    );

            rows.add(row);

            System.out.println(
                    "Q-table entries after query "
                            + queryNumber
                            + " = "
                            + row.qTableEntriesAfter()
            );
            System.out.flush();
        }

        QTableStore.save(
                qTable,
                ResearchSettings.qTablePath()
        );

        printSummary(rows, qTable);

        System.out.println(
                "\nMulti-query training complete. "
                        + "Set training.mode=inference to run queries "
                        + "using the shared Q-table."
        );

        return rows;
    }

    private static QTable loadOrCreateQTable() throws Exception {
        if (!ResearchSettings.resumeQTable()) {
            return new QTable();
        }

        File file =
                new File(
                        ResearchSettings.qTablePath()
                );

        if (!file.exists()) {
            return new QTable();
        }

        return QTableStore.load(
                ResearchSettings.qTablePath()
        );
    }

    private static void printSummary(
            List<WorkloadTrainRow> rows,
            QTable qTable
    ) {
        System.out.println(
                "\n===== WORKLOAD TRAINING SUMMARY ====="
        );

        System.out.printf(
                "%-4s %-28s %-8s %-10s%n",
                "#",
                "Query",
                "Cuts",
                "QEntries"
        );

        for (WorkloadTrainRow row : rows) {
            System.out.printf(
                    "%-4d %-28s %-8d %-10d%n",
                    row.queryNumber(),
                    row.queryName(),
                    row.executableCuts(),
                    row.qTableEntriesAfter()
            );
        }

        System.out.println(
                "\nFinal Q-table entries = "
                        + qTable.entries().size()
        );

        qTable.print();
    }
}
