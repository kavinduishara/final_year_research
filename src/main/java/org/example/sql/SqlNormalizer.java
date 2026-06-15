package org.example.sql;

/**
 * Normalizes user SQL before handing it to Calcite.
 *
 * <p>Calcite's parser accepts a single statement without a trailing semicolon.
 * Research workloads (Q1–Q10) often include trailing {@code ;} from editors.
 *
 * <p>Example:
 * <pre>
 *   input  = "SELECT c.mktsegment … GROUP BY c.mktsegment;\n"
 *   output = "SELECT c.mktsegment … GROUP BY c.mktsegment"
 * </pre>
 */
public final class SqlNormalizer {

    private SqlNormalizer() {
    }

    /**
     * Strips leading/trailing whitespace and removes trailing semicolons.
     *
     * @param sql raw query text, e.g. Q1 with optional {@code ;}
     * @return Calcite-safe single statement
     * @throws IllegalArgumentException if sql is null
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
