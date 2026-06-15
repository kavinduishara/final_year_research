package org.example.rl.bandit;

/**
 * Small dense-matrix helpers for LinUCB (no external linear algebra dependency).
 *
 * <p>Supports the operations in {@link ContextualBandit}:
 * A⁻¹b (parameter estimate), xᵀA⁻¹x (uncertainty), and A += xxᵀ (update).
 */
final class MatrixOps {

    private MatrixOps() {
    }

    /**
     * Matrix–vector product: result[i] = Σⱼ matrix[i][j] × vector[j].
     *
     * @param matrix d×d matrix (e.g. A⁻¹)
     * @param vector length d (e.g. b or x)
     * @return product vector, length d
     */
    static double[] multiply(
            double[][] matrix,
            double[] vector
    ) {
        int rows = matrix.length;
        double[] result = new double[rows];

        for (int i = 0; i < rows; i++) {
            double sum = 0.0;
            for (int j = 0; j < vector.length; j++) {
                sum += matrix[i][j] * vector[j];
            }
            result[i] = sum;
        }

        return result;
    }

    /**
     * Dot product of two equal-length vectors.
     *
     * @return Σᵢ left[i] × right[i], e.g. xᵀ(A⁻¹x) for uncertainty
     */
    static double dot(
            double[] left,
            double[] right
    ) {
        double sum = 0.0;
        for (int i = 0; i < left.length; i++) {
            sum += left[i] * right[i];
        }
        return sum;
    }

    /**
     * Rank-one update: matrix += vector × vectorᵀ (LinUCB A update).
     *
     * @param matrix d×d matrix modified in place
     * @param vector feature vector x from {@link CutFeatures}
     */
    static void addOuterProduct(
            double[][] matrix,
            double[] vector
    ) {
        for (int i = 0; i < vector.length; i++) {
            for (int j = 0; j < vector.length; j++) {
                matrix[i][j] +=
                        vector[i] * vector[j];
            }
        }
    }

    /**
     * @param source square matrix
     * @return deep copy
     */
    static double[][] copy(double[][] source) {
        double[][] copy =
                new double[source.length][source.length];

        for (int i = 0; i < source.length; i++) {
            System.arraycopy(
                    source[i],
                    0,
                    copy[i],
                    0,
                    source.length
            );
        }

        return copy;
    }

    /**
     * Gauss–Jordan inversion of a square matrix.
     *
     * @param matrix d×d matrix (e.g. A with ridge regularization)
     * @return inverse matrix A⁻¹
     */
    static double[][] invert(double[][] matrix) {
        int n = matrix.length;
        double[][] augmented = new double[n][2 * n];

        for (int i = 0; i < n; i++) {
            System.arraycopy(
                    matrix[i],
                    0,
                    augmented[i],
                    0,
                    n
            );
            augmented[i][n + i] = 1.0;
        }

        for (int col = 0; col < n; col++) {
            int pivot = col;
            for (int row = col + 1; row < n; row++) {
                if (Math.abs(augmented[row][col])
                        > Math.abs(augmented[pivot][col])) {
                    pivot = row;
                }
            }

            double[] temp = augmented[pivot];
            augmented[pivot] = augmented[col];
            augmented[col] = temp;

            double pivotValue = augmented[col][col];
            if (Math.abs(pivotValue) < 1e-12) {
                pivotValue = 1e-6;
                augmented[col][col] = pivotValue;
            }

            for (int j = 0; j < 2 * n; j++) {
                augmented[col][j] /= pivotValue;
            }

            for (int row = 0; row < n; row++) {
                if (row == col) {
                    continue;
                }

                double factor = augmented[row][col];
                for (int j = 0; j < 2 * n; j++) {
                    augmented[row][j] -=
                            factor * augmented[col][j];
                }
            }
        }

        double[][] inverse = new double[n][n];
        for (int i = 0; i < n; i++) {
            System.arraycopy(
                    augmented[i],
                    n,
                    inverse[i],
                    0,
                    n
            );
        }

        return inverse;
    }
}
