package org.example.rl.bandit;

/**
 * LinUCB contextual bandit: reward ≈ theta^T x for cut feature vector x.
 */
public final class ContextualBandit {

    private final double alpha;
    private final double ridge;
    private final double[][] aMatrix;
    private final double[] bVector;
    private int observations;

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

    public double alpha() {
        return alpha;
    }

    public double ridge() {
        return ridge;
    }

    public double[][] aMatrix() {
        return MatrixOps.copy(aMatrix);
    }

    public double[] bVector() {
        return bVector.clone();
    }

    public int observations() {
        return observations;
    }

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
