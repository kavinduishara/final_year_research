package org.example.rl.bandit;

import java.util.Random;

/**
 * Shared-feature contextual bandit (LinUCB or Thompson sampling).
 * One linear model: reward ≈ theta^T x where x describes a cut candidate.
 */
public final class ContextualBandit {

    private final BanditAlgorithm algorithm;
    private final double alpha;
    private final double ridge;
    private final double[][] aMatrix;
    private final double[] bVector;
    private int observations;
    private final Random random;

    public ContextualBandit(
            BanditAlgorithm algorithm,
            double alpha,
            double ridge
    ) {
        this.algorithm = algorithm;
        this.alpha = alpha;
        this.ridge = ridge;
        this.aMatrix = identityWithRidge(
                CutFeatures.DIMENSION,
                ridge
        );
        this.bVector = new double[CutFeatures.DIMENSION];
        this.observations = 0;
        this.random = new Random(
                42L
        );
    }

    private ContextualBandit(
            BanditAlgorithm algorithm,
            double alpha,
            double ridge,
            double[][] aMatrix,
            double[] bVector,
            int observations
    ) {
        this.algorithm = algorithm;
        this.alpha = alpha;
        this.ridge = ridge;
        this.aMatrix = aMatrix;
        this.bVector = bVector;
        this.observations = observations;
        this.random = new Random(
                42L
        );
    }

    public BanditAlgorithm algorithm() {
        return algorithm;
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
        return switch (algorithm) {
            case LINUCB -> linUcbScore(features);
            case THOMPSON -> thompsonScore(features);
        };
    }

    private double linUcbScore(double[] features) {
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

    private double thompsonScore(double[] features) {
        double[][] inverse =
                MatrixOps.invert(aMatrix);

        double[] mean =
                MatrixOps.multiply(
                        inverse,
                        bVector
                );

        double[] sample =
                sampleMultivariateNormal(
                        mean,
                        inverse
                );

        return MatrixOps.dot(
                sample,
                features
        );
    }

    private double[] sampleMultivariateNormal(
            double[] mean,
            double[][] covariance
    ) {
        double[] z = new double[mean.length];
        for (int i = 0; i < z.length; i++) {
            z[i] = random.nextGaussian();
        }

        double[][] chol =
                choleskyDecompose(covariance);

        double[] sample = new double[mean.length];
        for (int i = 0; i < mean.length; i++) {
            sample[i] = mean[i];
            for (int j = 0; j <= i; j++) {
                sample[i] +=
                        chol[i][j] * z[j];
            }
        }

        return sample;
    }

    private static double[][] choleskyDecompose(
            double[][] matrix
    ) {
        int n = matrix.length;
        double[][] lower = new double[n][n];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j <= i; j++) {
                double sum = matrix[i][j];
                for (int k = 0; k < j; k++) {
                    sum -= lower[i][k] * lower[j][k];
                }

                if (i == j) {
                    lower[i][j] =
                            Math.sqrt(
                                    Math.max(
                                            sum,
                                            1e-12
                                    )
                            );
                }
                else {
                    lower[i][j] =
                            sum / lower[j][j];
                }
            }
        }

        return lower;
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

    public static ContextualBandit createDefault(
            BanditAlgorithm algorithm
    ) {
        return new ContextualBandit(
                algorithm,
                0.5,
                1.0
        );
    }

    public static ContextualBandit restore(
            BanditAlgorithm algorithm,
            double alpha,
            double ridge,
            double[][] aMatrix,
            double[] bVector,
            int observations
    ) {
        return new ContextualBandit(
                algorithm,
                alpha,
                ridge,
                aMatrix,
                bVector,
                observations
        );
    }
}
