package org.example.rl;

import java.util.HashMap;
import java.util.Map;

public class QTable {

    private final Map<String, Double> table =
            new HashMap<>();

    public double get(String stateAction) {

        return table.getOrDefault(
                stateAction,
                0.0
        );
    }

    public void put(
            String stateAction,
            double value) {

        table.put(
                stateAction,
                value
        );
    }

    public Map<String, Double> entries() {
        return Map.copyOf(table);
    }

    public static QTable fromEntries(
            Map<String, Double> entries
    ) {
        QTable qTable = new QTable();
        qTable.table.putAll(entries);
        return qTable;
    }

    public void print() {

        System.out.println(
                "\n===== Q TABLE ====="
        );

        table.forEach(
                (k, v) ->
                        System.out.println(
                                k + " => " + v
                        )
        );
    }

    public void update(
            String key,
            double reward) {

        double alpha = 0.1;

        double oldValue =
                get(key);

        double newValue =
                oldValue +
                        alpha *
                                (reward - oldValue);

        put(key, newValue);
    }
}
