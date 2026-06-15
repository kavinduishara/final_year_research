package org.example.rl.bandit;

/**
 * LinUCB contextual bandit: reward ≈ θᵀx for cut feature vector x.
 *
 * <p>Each Q1 cut (38, 42) is an "arm" with context {@link CutFeatures#from(CutCandidate)}.
 * After executing a cut, {@link #update(double[], double)} learns from the measured reward.
 *
 * <p>Scoring (selection):
 * <pre>
 *   score(x) = θᵀx + α × sqrt(xᵀ A⁻¹ x)
 *   θ = A⁻¹b
 * </pre>
 *
 * <p>Example update after cut@38 on Q1:
 * <pre>
 *   features = CutFeatures.from(candidate@38)  // 10-dim vector
 *   reward   = RewardCalculator.executionReward(metrics)  // e.g. -2100
 *   update(features, reward)
 * </pre>
 */
public final class ContextualBandit {

    private final double alpha;
    private final double ridge;
    private final double[][] aMatrix;
    private final double[] bVector;
    private int observations;

    /**
     * Creates a fresh bandit with ridge-regularized identity A matrix.
     *
     * @param alpha exploration weight (UCB term multiplier)
     * @param ridge diagonal added to A at init, e.g. 1.0
     */
    public ContextualBandit(
            double alpha,
            double ridge
    ) {
        this.alpha = alpha;
        this.ridge = ridge;
        this.aMatrix = identityWithRidge(
                CutFeatures.DIMENSION,
                ridge
        );
        this.bVector = new double[CutFeatures.DIMENSION];
        this.observations = 0;
    }

    private ContextualBandit(
            double alpha,
            double ridge,
            double[][] aMatrix,
            double[] bVector,
            int observations
    ) {
        this.alpha = alpha;
        this.ridge = ridge;
        this.aMatrix = aMatrix;
        this.bVector = bVector;
        this.observations = observations;
    }

    /** @return UCB exploration parameter α */
    public double alpha() {
        return alpha;
    }

    /** @return ridge regularization λ */
    public double ridge() {
        return ridge;
    }

    /** @return defensive copy of the design matrix A */
    public double[][] aMatrix() {
        return MatrixOps.copy(aMatrix);
    }

    /** @return copy of the reward accumulator b */
    public double[] bVector() {
        return bVector.clone();
    }

    /** @return number of (context, reward) updates applied */
    public int observations() {
        return observations;
    }

    /**
     * LinUCB learning step: A += x xᵀ, b += reward × x.
     *
     * @param features context from {@link CutFeatures#from}, length 10
     * @param reward   execution reward (higher is better), e.g. -totalTimeMs scaled
     */
    public void update(
            double[] features,
            double reward
    ) {
        MatrixOps.addOuterProduct(
                aMatrix,
                features
        );

        for (int i = 0; i < bVector.length; i++) {
            bVector[i] +=
                    reward * features[i];
        }

        observations++;
    }

    /**
     * UCB score for selecting an arm: mean estimate + α × uncertainty.
     *
     * @param features cut context vector
     * @return scalar score; higher = more attractive arm for Q1 cut choice
     */
    public double score(double[] features) {
        double[][] inverse =
                MatrixOps.invert(aMatrix);

        double[] theta =
                MatrixOps.multiply(
                        inverse,
                        bVector
                );

        double mean =
                MatrixOps.dot(
                        theta,
                        features
                );

        double[] uncertaintyVector =
                MatrixOps.multiply(
                        inverse,
                        features
                );

        double uncertainty =
                Math.sqrt(
                        Math.max(
                                0.0,
                                MatrixOps.dot(
                                        features,
                                        uncertaintyVector
                                )
                        )
                );

        return mean + alpha * uncertainty;
    }

    /**
     * Reconstructs bandit from persisted snapshot (see {@link BanditStore}).
     */
    public static ContextualBandit restore(
            double alpha,
            double ridge,
            double[][] aMatrix,
            double[] bVector,
            int observations
    ) {
        return new ContextualBandit(
                alpha,
                ridge,
                aMatrix,
                bVector,
                observations
        );
    }

    private static double[][] identityWithRidge(
            int dimension,
            double ridge
    ) {
        double[][] matrix =
                new double[dimension][dimension];

        for (int i = 0; i < dimension; i++) {
            matrix[i][i] = ridge;
        }

        return matrix;
    }
}
