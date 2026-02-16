package org.example.sql;

import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.dialect.PostgresqlSqlDialect;

public final class PgSqlDialectFactory {
    private PgSqlDialectFactory() {}

    public static SqlDialect postgres() {
        return PostgresqlSqlDialect.DEFAULT;
    }
}
