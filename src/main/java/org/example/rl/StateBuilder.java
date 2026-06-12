package org.example.rl;

import org.example.plan.CutCandidate;

public class StateBuilder {

    public static State from(CutCandidate candidate) {

        String rows;

        if (candidate.estimatedRows() < 5000) {
            rows = "SMALL";
        }
        else if (candidate.estimatedRows() < 50000) {
            rows = "MEDIUM";
        }
        else {
            rows = "LARGE";
        }

        String depth;

        if (candidate.depth() <= 3) {
            depth = "SHALLOW";
        }
        else {
            depth = "DEEP";
        }

        String cost;

        if (candidate.estimatedCost() < 5000) {
            cost = "LOW_COST";
        }
        else if (candidate.estimatedCost() < 50000) {
            cost = "MEDIUM_COST";
        }
        else {
            cost = "HIGH_COST";
        }

        return new State(
                rows,
                depth,
                cost,
                candidate.localityBucket()
        );
    }
}
