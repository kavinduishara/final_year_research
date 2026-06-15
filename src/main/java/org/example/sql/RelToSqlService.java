package org.example.sql;

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.rel2sql.RelToSqlConverter;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlWriterConfig;
import org.apache.calcite.sql.pretty.SqlPrettyWriter;

/**
 * Converts a Calcite {@link RelNode} subtree into formatted SQL text.
 *
 * <p>Example Q1 fragment2 (after cut@42, reading {@code inter_single_n42}):
 * <pre>
 *   toSql(fragment2) →
 *     SELECT "c"."mktsegment", SUM("o"."totalprice")
 *     FROM "inter_single_n42" AS "t"
 *     …
 * </pre>
 */
public class RelToSqlService {

    private final SqlDialect dialect;

    /**
     * @param dialect target SQL dialect, typically PostgreSQL
     */
    public RelToSqlService(SqlDialect dialect) {
        this.dialect = dialect;
    }

    /**
     * RelNode → pretty-printed SQL string.
     *
     * @param rel logical plan subtree, e.g. fragment1 join or fragment2 aggregate
     * @return SQL without trailing semicolon, e.g. CREATE-compatible SELECT … FROM …
     */
    public String toSql(RelNode rel) {
        RelToSqlConverter converter = new RelToSqlConverter(dialect);
        SqlNode sqlNode = converter.visitRoot(rel).asStatement();

        SqlWriterConfig cfg = SqlPrettyWriter.config()
                .withDialect(dialect)
                .withSelectListItemsOnSeparateLines(true);

        SqlPrettyWriter writer = new SqlPrettyWriter(cfg);
        sqlNode.unparse(writer, 0, 0);
        return writer.toString();
    }
}
