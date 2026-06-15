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
 *
 * <p>Example training update after executing cut 42 on Q1:
 * <pre>
 *   observe(candidate(nodeId=42), reward=-3400.0)
 *   → bandit.update(CutFeatures.from(candidate), -3.4)  // reward scaled /1000
 * </pre>
 *
 * <p>Example inference:
 * <pre>
 *   loadForInference() → reads bandit.json
 *   choose(candidates) → CutSelection(Action(42), reason=LINUCB, bestScore=0.73)
 * </pre>
 */
public final class CutPolicyEngine {

    private final ContextualBandit bandit;

    private CutPolicyEngine(ContextualBandit bandit) {
        this.bandit = bandit;
    }

    /** @return new empty LinUCB model (alpha/ridge from ResearchSettings). */
    public static CutPolicyEngine createFresh() {
        return new CutPolicyEngine(
                new ContextualBandit(
                        ResearchSettings.banditAlpha(),
                        ResearchSettings.banditRidge()
                )
        );
    }

    /**
     * Load trained model for inference mode.
     * @return engine with bandit loaded from bandit.json (e.g. learning.bandit.path)
     */
    public static CutPolicyEngine loadForInference()
            throws Exception {
        return new CutPolicyEngine(
                BanditStore.load(
                        ResearchSettings.banditPath()
                )
        );
    }

    /**
     * Load or create model for training.
     * @param resume if true and bandit.json exists, continue updating it
     */
    public static CutPolicyEngine loadForTraining(
            boolean resume
    ) throws Exception {
        return new CutPolicyEngine(
                BanditStore.loadOrCreate(resume)
        );
    }

    /**
     * Record one cut's reward after distributed execution.
     *
     * @param candidate the cut that was executed
     * @param reward    e.g. -3400.0 (= -(runtimeMs + transferTimeMs))
     */
    public void observe(
            CutCandidate candidate,
            double reward
    ) {
        bandit.update(
                CutFeatures.from(candidate),
                reward / 1000.0
        );
    }

    /**
     * Pick best cut using bandit only (inference path).
     * @return CutSelection with highest LinUCB score among executable cuts
     */
    public CutSelection choose(
            List<CutCandidate> candidates
    ) {
        return choose(
                candidates,
                null
        );
    }

    /**
     * Pick best cut; after training may use observed execution times first.
     *
     * @param candidates           all enriched cut candidates for the query
     * @param metricsByCutNodeId   null in inference; Map{42→ExecutionMetrics, 38→…} after training
     * @return chosen cut + reason (EXECUTION_BEST if all cuts were tried in training)
     */
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

    /** Persist bandit to bandit.json (e.g. after training or benchmark). */
    public void save() throws Exception {
        BanditStore.save(
                bandit,
                ResearchSettings.banditPath()
        );
    }

    /** Print model stats (observations, alpha, ridge) to stdout. */
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

    /** @return underlying ContextualBandit (for tests/debug). */
    public ContextualBandit bandit() {
        return bandit;
    }

    /** @return display label "LINUCB". */
    public String algorithmLabel() {
        return "LINUCB";
    }
}
