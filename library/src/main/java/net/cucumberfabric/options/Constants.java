package net.cucumberfabric.options;

import net.fabricmc.api.EnvType;
import org.junit.platform.engine.ConfigurationParameters;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configuration property names and defaults for the Cucumber-Fabric
 * server engine.
 */
public final class Constants {

    private Constants() { /* prevent instantiation */ }

    // PROPERTY-NAME CONSTANTS

    /**
     * Property name to set the root run directory for both
     * server and client artifacts.
     * <p>
     * Accepts a file-system path.
     * Default: {@value #ROOT_DEFAULT_RUN_DIR}
     * <br>
     * Note: This folder will be created if it doesn't exist.
     */
    public static final String ROOT_RUN_DIR_PROPERTY_NAME =
            "cucumberfabric.root-run-dir";

    /**
     * Property name to set the server or client run
     * server and client artifacts.
     * <p>
     * Accepts a file-system path.
     * Default: {@value #ROOT_DEFAULT_RUN_DIR}
     * <br>
     * Note: This folder will be created if it doesn't exist.
     */
    public static final String ENVTYPE_PROPERTY_NAME =
            "cucumberfabric.envtype";


    /**
     * Property name to set the run directory.
     * <p>
     * Accepts a file-system path.
     * Default: {@value #DEFAULT_RUN_DIR}
     * <br>
     * Note: This folder will be created (inside {@value #ROOT_DEFAULT_RUN_DIR}) if it doesn't exist.
     */
    public static final String RUN_DIR_PROPERTY_NAME =
            "cucumberfabric.run-dir";

    /**
     * Property name to control whether the server state is saved on stop.
     * <p>
     * Accepts "true" or "false".
     * Default: {@value #DEFAULT_SAVE_ON_STOP}
     */
    public static final String SAVE_ON_STOP_PROPERTY_NAME =
            "cucumberfabric.save-on-stop";

    /**
     * Property name to control deletion of data when the tests finish.
     * <p>
     * Accepts "true" or "false".
     * Default: {@value #DEFAULT_DELETE_ON_FINISH}
     */
    public static final String DELETE_ON_FINISH_PROPERTY_NAME =
            "cucumberfabric.delete-on-finish";

    /**
     * Property name to select which Cucumber engine to load in your runner.
     * <p>
     * Accepts the engine ID string (e.g. "cucumber", "fabric", etc.).
     * Default: {@value #DEFAULT_ENGINE_TO_USE}
     */
    public static final String ENGINE_TO_USE_PROPERTY_NAME =
            "cucumberfabric.engine-to-use";


    // DEFAULT VALUES

    /**
     * Root directory which is used to run all cucumber artifacts.
     * <p>
     * System property: {@code ROOT_RUN_DIR_PROPERTY_NAME}, default "run_cucumber".
     */
    public static final String ROOT_DEFAULT_RUN_DIR = "run_cucumber";

    /**
     * EnvType for the type of test run (server or client)
     * <p>
     * System property: {@code ROOT_RUN_DIR_PROPERTY_NAME}, default "run_cucumber".
     */
    public static final String DEFAULT_ENVTYPE = "server";

    /**
     * Default subdirectory under root for server artifacts.
     * <p>
     * System property: {@code RUN_DIR_PROPERTY_NAME}, default "server".
     */
    public static final String DEFAULT_RUN_DIR = "server";

    /**
     * By default, server state will be persisted when stopped.
     */
    public static final boolean DEFAULT_SAVE_ON_STOP = true;

    /**
     * By default, server artifacts are not deleted on finish.
     */
    public static final boolean DEFAULT_DELETE_ON_FINISH = false;

    /**
     * Default Cucumber engine ID to use in your custom runner.
     */
    public static final String DEFAULT_ENGINE_TO_USE = "cucumber";


    // GENERIC System.getProperty–BASED getters

    public static String getOrDefault(String propertyName,
                                      String defaultValue) {
        String v = System.getProperty(propertyName);
        return (v != null && !v.isBlank()) ? v : defaultValue;
    }

    public static boolean getOrDefault(String propertyName,
                                       boolean defaultValue) {
        String v = System.getProperty(propertyName);
        return (v != null && !v.isBlank())
                ? Boolean.parseBoolean(v)
                : defaultValue;
    }


    // GENERIC ConfigurationParameters–BASED getters

    public static String getOrDefault(ConfigurationParameters params,
                                      String propertyName,
                                      String defaultValue) {
        return params.get(propertyName)
                .filter(s -> !s.isBlank())
                .orElse(defaultValue);
    }

    public static boolean getOrDefault(ConfigurationParameters params,
                                       String propertyName,
                                       boolean defaultValue) {
        return params.get(propertyName)
                .filter(s -> !s.isBlank())
                .map(Boolean::parseBoolean)
                .orElse(defaultValue);
    }


    // CONVENIENCE METHODS (System properties)

    /**
     * @return root run directory (system property or {@value #ROOT_DEFAULT_RUN_DIR})
     */
    public static String getRootRunDir() {
        return getOrDefault(ROOT_RUN_DIR_PROPERTY_NAME, ROOT_DEFAULT_RUN_DIR);
    }

    /**
     * @return server run directory name (system property or {@value #DEFAULT_ENVTYPE})
     */
    public static String getEnvTypeString() {
        return getOrDefault(ENVTYPE_PROPERTY_NAME, DEFAULT_ENVTYPE);
    }

    /**
     * @return server run directory name (system property or {@value #DEFAULT_RUN_DIR})
     */
    public static EnvType getEnvType() {
        return EnvType.valueOf(getOrDefault(ENVTYPE_PROPERTY_NAME, DEFAULT_ENVTYPE).toUpperCase());
    }

    /**
     * @return full server run path: {root} + {server}
     */
    public static Path getRunPath() {
        return Paths.get(
                getRootRunDir(),
                getOrDefault(RUN_DIR_PROPERTY_NAME, DEFAULT_RUN_DIR)
        );
    }

    /**
     * @return server run directory name (system property or {@value #DEFAULT_RUN_DIR})
     */
    public static String getRunDir() {
        return getOrDefault(RUN_DIR_PROPERTY_NAME, DEFAULT_RUN_DIR);
    }

    /**
     * @return whether to save on stop (system property or {@value #DEFAULT_SAVE_ON_STOP})
     */
    public static boolean isSaveOnStop() {
        return getOrDefault(SAVE_ON_STOP_PROPERTY_NAME, DEFAULT_SAVE_ON_STOP);
    }

    /**
     * @return whether to delete on finish (system property or {@value #DEFAULT_DELETE_ON_FINISH})
     */
    public static boolean isDeleteOnFinish() {
        return getOrDefault(DELETE_ON_FINISH_PROPERTY_NAME, DEFAULT_DELETE_ON_FINISH);
    }

    /**
     * @return engine ID to use (system property or {@value #DEFAULT_ENGINE_TO_USE})
     */
    public static String getEngineToUse() {
        return getOrDefault(ENGINE_TO_USE_PROPERTY_NAME, DEFAULT_ENGINE_TO_USE);
    }


    // CONVENIENCE METHODS (ConfigurationParameters)

    /**
     * @see #getRootRunDir()
     */
    public static String getRootRunDir(ConfigurationParameters params) {
        return getOrDefault(params,
                ROOT_RUN_DIR_PROPERTY_NAME,
                ROOT_DEFAULT_RUN_DIR);
    }

    /**
     * @see #getRunDir()
     */
    public static String getRunDir(ConfigurationParameters params) {
        return getOrDefault(params,
                RUN_DIR_PROPERTY_NAME,
                DEFAULT_RUN_DIR);
    }

    /**
     * @see #isSaveOnStop()
     */
    public static boolean isSaveOnStop(ConfigurationParameters params) {
        return getOrDefault(params,
                SAVE_ON_STOP_PROPERTY_NAME,
                DEFAULT_SAVE_ON_STOP);
    }

    /**
     * @see #isDeleteOnFinish()
     */
    public static boolean isDeleteOnFinish(ConfigurationParameters params) {
        return getOrDefault(params,
                DELETE_ON_FINISH_PROPERTY_NAME,
                DEFAULT_DELETE_ON_FINISH);
    }

    /**
     * @see #getEngineToUse()
     */
    public static String getEngineToUse(ConfigurationParameters params) {
        return getOrDefault(params,
                ENGINE_TO_USE_PROPERTY_NAME,
                DEFAULT_ENGINE_TO_USE);
    }

    /**
     * Take an existing map of config-string→string, and ensure every
     * Constants.* key is present (using either the provided value
     * or the Constants.DEFAULT_* fallback).
     */
    public static Map<String, String> mergeWithDefaults(Map<String, String> provided) {
        Map<String, String> merged = new LinkedHashMap<>(provided);

        merged.put(ROOT_RUN_DIR_PROPERTY_NAME,
                getRootRunDir());

        merged.put(RUN_DIR_PROPERTY_NAME,
                getRunDir());

        merged.put(ENGINE_TO_USE_PROPERTY_NAME,
                getEngineToUse());

        merged.put(SAVE_ON_STOP_PROPERTY_NAME,
                String.valueOf(isSaveOnStop()));

        return merged;
    }
}
