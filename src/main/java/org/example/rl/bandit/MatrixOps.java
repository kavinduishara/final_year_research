package org.example.rl.bandit;

final class MatrixOps {

    private MatrixOps() {
    }

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
