package org.example.rl.bandit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.config.ResearchSettings;

import java.io.File;

/**
 * Persists and restores the LinUCB model (A matrix, b vector, observation count).
 *
 * <p>After multi-query training on Q1–Q10, saves to the path in
 * {@link ResearchSettings#banditPath()} so inference can resume without re-executing cuts.
 *
 * <p>Example snapshot:
 * <pre>
 *   { "algorithm": "linucb", "observations": 40,
 *     "dimensions": 10, "alpha": 0.5, "ridge": 1.0,
 *     "a": [[…]], "b": […] }
 * </pre>
 */
public final class BanditStore {

    private static final ObjectMapper MAPPER =
            new ObjectMapper();

    /**
     * Serializable bandit state written to JSON.
     *
     * @param algorithm     always "linucb"
     * @param alpha         exploration parameter
     * @param ridge         diagonal regularization on A
     * @param dimensions    feature vector size ({@link CutFeatures#DIMENSION})
     * @param observations  number of (feature, reward) updates
     * @param a             d×d design matrix
     * @param b             d×1 reward-weighted sum
     */
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

    /**
     * Writes the current bandit parameters to a JSON file.
     *
     * @param bandit trained model after Q1 episodes
     * @param path   output file, e.g. bandit-model.json
     */
    public static void save(
            ContextualBandit bandit,
            String path
    ) throws Exception {
        BanditSnapshot snapshot =
                new BanditSnapshot(
                        "linucb",
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
                "Saved LinUCB model to "
                        + path
                        + " ("
                        + bandit.observations()
                        + " observations)"
        );
    }

    /**
     * Loads a previously saved model; fails if file missing.
     *
     * @param path JSON model file
     * @return restored {@link ContextualBandit}
     * @throws IllegalStateException if file does not exist
     */
    public static ContextualBandit load(String path)
            throws Exception {

        File file = new File(path);

        if (!file.exists()) {
            throw new IllegalStateException(
                    "Bandit model file not found: "
                            + path
                            + ". Run training first."
            );
        }

        BanditSnapshot snapshot =
                MAPPER.readValue(
                        file,
                        BanditSnapshot.class
                );

        ContextualBandit bandit =
                ContextualBandit.restore(
                        snapshot.alpha(),
                        snapshot.ridge(),
                        snapshot.a(),
                        snapshot.b(),
                        snapshot.observations()
                );

        System.out.println(
                "Loaded LinUCB model from "
                        + path
                        + " ("
                        + snapshot.observations()
                        + " observations)"
        );

        return bandit;
    }

    /**
     * Loads existing model when {@code resume=true} and file exists; otherwise fresh bandit.
     *
     * @param resume whether to continue from saved weights
     * @return bandit ready for training or inference
     */
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
                ResearchSettings.banditAlpha(),
                ResearchSettings.banditRidge()
        );
    }
}
