package org.example.rl.bandit;

import org.example.plan.CutCandidate;

/**
 * Numeric feature vector for one executable cut (contextual bandit arm).
 */
public final class CutFeatures {

    public static final int DIMENSION = 10;

    private CutFeatures() {
    }

    public static double[] from(CutCandidate candidate) {
        double[] x = new double[DIMENSION];

        x[0] = 1.0;
        x[1] = scaleLog(candidate.estimatedRows());
        x[2] = candidate.depth() / 10.0;
        x[3] = scaleLog(candidate.estimatedCost());
        x[4] = scaleLog(candidate.baseTableTransferCost());
        x[5] = scaleLog(candidate.intermediateTransferCost());
        x[6] = scaleLog(candidate.totalTransferCost());
        x[7] = bucketFlag(
                candidate.localityBucket(),
                "SHIPPING_REQUIRED"
        );
        x[8] = bucketFlag(
                candidate.localityBucket(),
                "INTERMEDIATE_ONLY"
        );
        x[9] = candidate.depth() > 3 ? 1.0 : 0.0;

        return x;
    }

    private static double scaleLog(double value) {
        if (value < 0) {
            return 0.0;
        }
        return Math.log1p(value) / 10.0;
    }

    private static double bucketFlag(
            String bucket,
            String expected
    ) {
        return expected.equals(bucket) ? 1.0 : 0.0;
    }
}
