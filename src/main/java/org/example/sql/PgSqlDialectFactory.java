package org.example.sql;

import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.dialect.PostgresqlSqlDialect;

/**
 * Factory for the PostgreSQL SQL dialect used when generating worker SQL.
 *
 * <p>All distributed execution targets PostgreSQL on worker1/worker2, so
 * fragment SQL from Q1 cuts uses {@link PostgresqlSqlDialect#DEFAULT}.
 */
public final class PgSqlDialectFactory {

    private PgSqlDialectFactory() {}

    /**
     * @return Calcite PostgreSQL dialect for {@link RelToSqlService} and {@link FragmentSqlBuilder}
     */
    public static SqlDialect postgres() {
        return PostgresqlSqlDialect.DEFAULT;
    }
}
