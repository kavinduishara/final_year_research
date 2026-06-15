package org.example.rl;

import org.example.plan.CutCandidate;

import java.util.List;

/**
 * Strategy interface for choosing which JOIN cut to execute.
 *
 * <p>Implementations compare enriched {@link CutCandidate} records for Q1
 * (cuts at nodes 38 and 42) and return an {@link Action} with the winning node id.
 *
 * <p>Example:
 * <pre>
 *   StaticQoSBaselinePolicy.choose(candidates) → Action(38)  // lowest transfer
 *   WeightedShipBaselinePolicy.choose(candidates) → Action(42) // prefers deep cut
 * </pre>
 */
public interface CutPolicy {

    /**
     * Selects one executable cut from the candidate list.
     *
     * @param candidates enriched cuts with locality and cost fields
     * @return action wrapping chosen RelNode id, e.g. Action(38)
     */
    Action choose(
            List<CutCandidate> candidates
    );
}
