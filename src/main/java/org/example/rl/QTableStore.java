package org.example.rl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class QTableStore {

    private static final ObjectMapper MAPPER =
            new ObjectMapper();

    public static void save(
            QTable qTable,
            String path
    ) throws Exception {
        MAPPER.writerWithDefaultPrettyPrinter()
                .writeValue(
                        new File(path),
                        qTable.entries()
                );

        System.out.println(
                "Saved Q-table to "
                        + path
        );
    }

    public static QTable load(String path) throws Exception {
        File file = new File(path);

        if (!file.exists()) {
            throw new IllegalStateException(
                    "Q-table file not found: "
                            + path
                            + ". Run with training.mode=train first."
            );
        }

        Map<String, Double> entries =
                MAPPER.readValue(
                        file,
                        new TypeReference<>() {}
                );

        System.out.println(
                "Loaded Q-table from "
                        + path
                        + " ("
                        + entries.size()
                        + " entries)"
        );

        return QTable.fromEntries(entries);
    }
}
