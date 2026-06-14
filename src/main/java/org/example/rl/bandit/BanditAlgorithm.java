package org.example.rl.bandit;

public enum BanditAlgorithm {
    LINUCB,
    THOMPSON;

    public static BanditAlgorithm fromConfig(String value) {
        if (value == null) {
            return LINUCB;
        }

        return switch (value.trim().toLowerCase()) {
            case "thompson", "ts" -> THOMPSON;
            default -> LINUCB;
        };
    }
}
