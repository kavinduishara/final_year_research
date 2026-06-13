package org.example.sql;

public final class SqlNormalizer {

    private SqlNormalizer() {
    }

    /**
     * Prepares SQL for Calcite, which accepts a single statement without a trailing semicolon.
     */
    public static String forCalcite(String sql) {

        if (sql == null) {
            throw new IllegalArgumentException(
                    "SQL must not be null"
            );
        }

        String normalized =
                sql.strip();

        while (normalized.endsWith(";")) {
            normalized =
                    normalized.substring(
                            0,
                            normalized.length() - 1
                    ).strip();
        }

        return normalized;
    }
}
