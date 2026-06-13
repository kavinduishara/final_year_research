package org.example.experiment;

public record ExecutionSession(String id) {

    public static ExecutionSession forQuery(int queryNumber) {
        return new ExecutionSession(
                "q" + queryNumber
        );
    }

    public static ExecutionSession single() {
        return new ExecutionSession(
                "single"
        );
    }

    public String intermediateTableName(int cutNodeId) {
        return "inter_"
                + id
                + "_n"
                + cutNodeId;
    }
}
