package org.example.rl.bandit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.config.ResearchSettings;

import java.io.File;

public final class BanditStore {

    private static final ObjectMapper MAPPER =
            new ObjectMapper();

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record BanditSnapshot(
            String algorithm,
            double alpha,
            double ridge,
            int dimensions,
            int observations,
            double[][] a,
            double[] b
    ) {
    }

    private BanditStore() {
    }

    public static void save(
            ContextualBandit bandit,
            String path
    ) throws Exception {
        BanditSnapshot snapshot =
                new BanditSnapshot(
                        bandit.algorithm()
                                .name()
                                .toLowerCase(),
                        bandit.alpha(),
                        bandit.ridge(),
                        CutFeatures.DIMENSION,
                        bandit.observations(),
                        bandit.aMatrix(),
                        bandit.bVector()
                );

        MAPPER.writerWithDefaultPrettyPrinter()
                .writeValue(
                        new File(path),
                        snapshot
                );

        System.out.println(
                "Saved contextual bandit to "
                        + path
                        + " ("
                        + bandit.observations()
                        + " observations, "
                        + bandit.algorithm()
                        + ")"
        );
    }

    public static ContextualBandit load(String path)
            throws Exception {

        File file = new File(path);

        if (!file.exists()) {
            throw new IllegalStateException(
                    "Bandit model file not found: "
                            + path
                            + ". Run training with "
                            + "learning.algorithm=linucb first."
            );
        }

        BanditSnapshot snapshot =
                MAPPER.readValue(
                        file,
                        BanditSnapshot.class
                );

        BanditAlgorithm algorithm =
                BanditAlgorithm.fromConfig(
                        snapshot.algorithm()
                );

        ContextualBandit bandit =
                ContextualBandit.restore(
                        algorithm,
                        snapshot.alpha(),
                        snapshot.ridge(),
                        snapshot.a(),
                        snapshot.b(),
                        snapshot.observations()
                );

        System.out.println(
                "Loaded contextual bandit from "
                        + path
                        + " ("
                        + snapshot.observations()
                        + " observations, "
                        + algorithm
                        + ")"
        );

        return bandit;
    }

    public static ContextualBandit loadOrCreate(
            boolean resume
    ) throws Exception {
        String path =
                ResearchSettings.banditPath();

        File file = new File(path);

        if (resume && file.exists()) {
            return load(path);
        }

        return new ContextualBandit(
                ResearchSettings.banditAlgorithm(),
                ResearchSettings.banditAlpha(),
                ResearchSettings.banditRidge()
        );
    }
}
