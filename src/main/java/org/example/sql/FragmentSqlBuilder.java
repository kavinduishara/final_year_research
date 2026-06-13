package org.example.sql;

import org.apache.calcite.sql.SqlDialect;
import org.example.split.SplitResult;

public class FragmentSqlBuilder {

    private final RelToSqlService toSql;

    public FragmentSqlBuilder(SqlDialect dialect) {
        this.toSql = new RelToSqlService(dialect);
    }

    public FragmentSql build(SplitResult split) {
        String placeholder = split.placeholderName();
        String quoted =
                quoteIdentifier(placeholder);

        String rawSql =
                toSql.toSql(
                        split.fragment1()
                );

        String projection =
                DynamicProjectionBuilder.build(
                        split.fragment1()
                );

        int fromPos =
                rawSql.indexOf("FROM");

        String frag1Select =
                projection
                        + "\n"
                        + rawSql.substring(fromPos);

        String dropSql =
                "DROP TABLE IF EXISTS "
                        + quoted
                        + " CASCADE";

        String createSql =
                "CREATE TABLE "
                        + quoted
                        + " AS\n"
                        + frag1Select;

        String sql2 =
                toSql.toSql(split.fragment2()) + ";";

        return new FragmentSql(
                dropSql,
                createSql,
                sql2,
                placeholder
        );
    }

    private static String quoteIdentifier(String name) {
        return "\"" + name + "\"";
    }
}
