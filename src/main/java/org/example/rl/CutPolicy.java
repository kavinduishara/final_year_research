package org.example.rl;

import org.example.plan.CutCandidate;

import java.util.List;

public interface CutPolicy {

    Action choose(
            List<CutCandidate> candidates
    );
}
