package org.example.rl;



import org.example.plan.CutCandidate;



import java.util.List;



public class QLearningPolicy {



    private final CutPolicyEngine policyEngine;



    public QLearningPolicy(CutPolicyEngine policyEngine) {

        this.policyEngine = policyEngine;

    }



    public QLearningPolicy(QTable qTable) {
        this.policyEngine = CutPolicyEngine.fromQTable(qTable);
    }



    public Action choose(List<CutCandidate> candidates) {

        return policyEngine.choose(

                candidates

        ).action();

    }



    /**

     * State-only key so Q-values generalize across queries

     * (Calcite node ids differ per plan).

     */

    public static String stateKey(State state) {

        return state.rowBucket()

                + "_"

                + state.depthBucket()

                + "_"

                + state.costBucket()

                + "_"

                + state.localityBucket();

    }

}

