package org.example.sql;

public class IntermediateSqlFixer {

    public static String fix(String sql) {

        return sql.replace(
                "SELECT *",
                """
                        SELECT
                            customer.custkey AS custkey,
                            customer.name AS name,
                            customer.address AS address,
                            customer.nationkey AS nationkey,
                            customer.phone AS phone,
                            customer.acctbal AS acctbal,
                            customer.mktsegment AS mktsegment,
                            customer.comment AS comment,
                        
                            orders.orderkey AS orderkey,
                            orders.custkey AS custkey0,
                            orders.orderstatus AS orderstatus,
                            orders.totalprice AS totalprice,
                            orders.orderdate AS orderdate,
                            orders.orderpriority AS orderpriority,
                            orders.clerk AS clerk,
                            orders.shippriority AS shippriority,
                            orders.comment AS comment0
                """
        );
    }
}