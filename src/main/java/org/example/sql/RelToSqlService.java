package org.example.sql;

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.rel2sql.RelToSqlConverter;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlWriterConfig;
import org.apache.calcite.sql.pretty.SqlPrettyWriter;

public class RelToSqlService {

    private final SqlDialect dialect;

    public RelToSqlService(SqlDialect dialect) {
        this.dialect = dialect;
    }

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
