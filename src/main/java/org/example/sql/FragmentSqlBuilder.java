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

        // Fragment 1 -> SELECT ...
//        String frag1Select = toSql.toSql(split.fragment1());
        String frag1Select =
                IntermediateSqlFixer.fix(
                        toSql.toSql(
                                split.fragment1()
                        )
                );
        String sql1 =
                "DROP TABLE IF EXISTS "
                        + placeholder
                        + ";\n"
                        + "CREATE TABLE "
                        + placeholder
                        + " AS\n"
                        + frag1Select
                        + ";";

        // Fragment 2 -> SELECT ... FROM <placeholder> ...
        String sql2 = toSql.toSql(split.fragment2()) + ";";

        return new FragmentSql(sql1, sql2);
    }
}
