package org.example.sql;

public record FragmentSql(
        String dropSql,
        String createSql,
        String sql2,
        String tempTableName
) {
    public String sql1() {
        return dropSql
                + "\n"
                + createSql;
    }
}
