package org.example.rl;

import org.example.config.ResearchSettings;
import org.example.exec.ExecutionMetrics;
import org.example.plan.CutCandidate;
import org.example.rl.bandit.BanditCutSelector;
import org.example.rl.bandit.BanditStore;
import org.example.rl.bandit.ContextualBandit;
import org.example.rl.bandit.CutFeatures;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Unified cut-selection + learning facade for Q-table or contextual bandit.
 */
public final class CutPolicyEngine {

    private final QTable qTable;
    private final ContextualBandit bandit;

    private CutPolicyEngine(
            QTable qTable,
            ContextualBandit bandit
    ) {
        this.qTable = qTable;
        this.bandit = bandit;
    }

    public static CutPolicyEngine fromQTable(QTable qTable) {
        return new CutPolicyEngine(
                qTable,
                null
        );
    }

    public static CutPolicyEngine createFresh() {
        if (ResearchSettings.usesBandit()) {
            return new CutPolicyEngine(
                    null,
                    new ContextualBandit(
                            ResearchSettings.banditAlgorithm(),
                            ResearchSettings.banditAlpha(),
                            ResearchSettings.banditRidge()
                    )
            );
        }

        return new CutPolicyEngine(
                new QTable(),
                null
        );
    }

    public static CutPolicyEngine loadForInference()
            throws Exception {

        if (ResearchSettings.usesBandit()) {
            return new CutPolicyEngine(
                    null,
                    BanditStore.load(
                            ResearchSettings.banditPath()
                    )
            );
        }

        return new CutPolicyEngine(
                QTableStore.load(
                        ResearchSettings.qTablePath()
                ),
                null
        );
    }

    public static CutPolicyEngine loadForTraining(
            boolean resume
    ) throws Exception {

        if (ResearchSettings.usesBandit()) {
            return new CutPolicyEngine(
                    null,
                    BanditStore.loadOrCreate(resume)
            );
        }

        if (resume) {
            File file =
                    new File(
                            ResearchSettings.qTablePath()
                    );

            if (file.exists()) {
                return new CutPolicyEngine(
                        QTableStore.load(
                                ResearchSettings.qTablePath()
                        ),
                        null
                );
            }
        }

        return createFresh();
    }

    public void observe(
            CutCandidate candidate,
            double reward
    ) {
        if (bandit != null) {
            bandit.update(
                    CutFeatures.from(candidate),
                    reward / 1000.0
            );
            return;
        }

        qTable.update(
                QLearningPolicy.stateKey(
                        StateBuilder.from(candidate)
                ),
                reward
        );
    }

    public CutSelection choose(
            List<CutCandidate> candidates
    ) {
        return choose(
                candidates,
                null
        );
    }

    public CutSelection choose(
            List<CutCandidate> candidates,
            Map<Integer, ExecutionMetrics> metricsByCutNodeId
    ) {
        if (ResearchSettings.useExecutionBestAfterTraining()
                && metricsByCutNodeId != null) {

            CutSelection observed =
                    ObservedRewardSelector
                            .chooseIfFullyObserved(
                                    candidates,
                                    metricsByCutNodeId
                            );

            if (observed != null) {
                return observed;
            }
        }

        if (bandit != null) {
            return BanditCutSelector.choose(
                    candidates,
                    bandit
            );
        }

        return CutSelector.choose(
                candidates,
                qTable
        );
    }

    public void save() throws Exception {
        if (bandit != null) {
            BanditStore.save(
                    bandit,
                    ResearchSettings.banditPath()
            );
            return;
        }

        QTableStore.save(
                qTable,
                ResearchSettings.qTablePath()
        );
    }

    public void print() {
        if (bandit != null) {
            System.out.println(
                    "\n===== CONTEXTUAL BANDIT ====="
            );
            System.out.println(
                    "Algorithm     = "
                            + bandit.algorithm()
            );
            System.out.println(
                    "Observations  = "
                            + bandit.observations()
            );
            System.out.println(
                    "Alpha         = "
                            + bandit.alpha()
            );
            System.out.println(
                    "Ridge         = "
                            + bandit.ridge()
            );
            return;
        }

        qTable.print();
    }

    public QTable qTable() {
        return qTable;
    }

    public ContextualBandit bandit() {
        return bandit;
    }

    public String algorithmLabel() {
        if (bandit != null) {
            return bandit.algorithm().name();
        }
        return "Q-TABLE";
    }
}
