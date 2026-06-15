package org.example.rl.bandit;

import org.example.plan.CutCandidate;

/**
 * Numeric feature vector for one executable cut (contextual bandit arm).
 *
 * <p>Maps a Q1 {@link CutCandidate} to a 10-dimensional context for LinUCB.
 * Each cut (38 vs 42) gets a different vector; the bandit learns which
 * features predict low execution time.
 *
 * <p>Example cut@38 (shallow, worker1/worker2, INTERMEDIATE_ONLY):
 * <pre>
 *   x[0] = 1.0           (bias)
 *   x[1] = log1p(rows)/10 ≈ log1p(45000)/10
 *   x[2] = depth/10      = 0.2
 *   x[6] = log1p(totalTransfer)/10
 *   x[8] = 1.0           (INTERMEDIATE_ONLY flag)
 * </pre>
 */
public final class CutFeatures {

    /** Length of the context vector passed to {@link ContextualBandit}. */
    public static final int DIMENSION = 10;

    private CutFeatures() {
    }

    /**
     * Builds the LinUCB context vector from one enriched cut candidate.
     *
     * @param candidate Q1 cut with locality and cost fields populated
     * @return double[10] feature array for score/update
     */
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

    /**
     * log1p scaling for wide dynamic range (row counts, byte costs).
     *
     * @param value raw statistic; negative treated as 0
     * @return log1p(value) / 10
     */
    private static double scaleLog(double value) {
        if (value < 0) {
            return 0.0;
        }
        return Math.log1p(value) / 10.0;
    }

    /**
     * One-hot style flag for a {@link CutCandidate#localityBucket()} value.
     *
     * @return 1.0 if bucket matches expected, else 0.0
     */
    private static double bucketFlag(
            String bucket,
            String expected
    ) {
        return expected.equals(bucket) ? 1.0 : 0.0;
    }
}
