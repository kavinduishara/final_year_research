package org.example.rl;

import org.example.config.ResearchSettings;
import org.example.exec.ExecutionMetrics;
import org.example.plan.CutCandidate;
import org.example.rl.bandit.BanditCutSelector;
import org.example.rl.bandit.BanditStore;
import org.example.rl.bandit.ContextualBandit;
import org.example.rl.bandit.CutFeatures;

import java.util.List;
import java.util.Map;

/**
 * LinUCB cut-selection and training facade.
 */
public final class CutPolicyEngine {

    private final ContextualBandit bandit;

    private CutPolicyEngine(ContextualBandit bandit) {
        this.bandit = bandit;
    }

    public static CutPolicyEngine createFresh() {
        return new CutPolicyEngine(
                new ContextualBandit(
                        ResearchSettings.banditAlpha(),
                        ResearchSettings.banditRidge()
                )
        );
    }

    public static CutPolicyEngine loadForInference()
            throws Exception {
        return new CutPolicyEngine(
                BanditStore.load(
                        ResearchSettings.banditPath()
                )
        );
    }

    public static CutPolicyEngine loadForTraining(
            boolean resume
    ) throws Exception {
        return new CutPolicyEngine(
                BanditStore.loadOrCreate(resume)
        );
    }

    public void observe(
            CutCandidate candidate,
            double reward
    ) {
        bandit.update(
                CutFeatures.from(candidate),
                reward / 1000.0
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

        return BanditCutSelector.choose(
                candidates,
                bandit
        );
    }

    public void save() throws Exception {
        BanditStore.save(
                bandit,
                ResearchSettings.banditPath()
        );
    }

    public void print() {
        System.out.println(
                "\n===== LINUCB MODEL ====="
        );
        System.out.println(
                "Observations = "
                        + bandit.observations()
        );
        System.out.println(
                "Alpha        = "
                        + bandit.alpha()
        );
        System.out.println(
                "Ridge        = "
                        + bandit.ridge()
        );
    }

    public ContextualBandit bandit() {
        return bandit;
    }

    public String algorithmLabel() {
        return "LINUCB";
    }
}
