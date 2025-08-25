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
     * Default: {@value #ROOT_RUN_ROOT_RUN_DIR}
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
     * Default: {@value #ROOT_RUN_ROOT_RUN_DIR}
     * <br>
     * Note: This folder will be created if it doesn't exist.
     */
    public static final String ENVTYPE_PROPERTY_NAME =
            "cucumberfabric.envtype";


    /**
     * Property name to set the run directory.
     * <p>
     * Accepts a file-system path.
     * Default: {@value #DEFAULT_SERVER_RUN_DIR}
     * <br>
     * Note: This folder will be created (inside {@value #ROOT_RUN_ROOT_RUN_DIR}) if it doesn't exist.
     */
    public static final String RUN_SERVER_DIR_PROPERTY_NAME =
            "cucumberfabric.server-run-dir";

    /**
     * Property name to set the run directory.
     * <p>
     * Accepts a file-system path.
     * Default: {@value #DEFAULT_CLIENT_RUN_DIR}
     * <br>
     * Note: This folder will be created (inside {@value #ROOT_RUN_ROOT_RUN_DIR}) if it doesn't exist.
     */
    public static final String RUN_CLIENT_DIR_PROPERTY_NAME =
            "cucumberfabric.client-run-dir";


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


    /**
     * Property name to set how long it will take before the FabricEngine will time-out (in seconds).
     * <p>
     * Accepts a int value representing seconds
     * Default: {@value #DEFAULT_ENGINE_TIMEOUT}
     */
    public static final String ENGINE_TIMEOUT_PROPERTY_NAME =
            "cucumberfabric.engine-time-out";
    // DEFAULT VALUES

    /**
     * Root directory which is used to run all cucumber artifacts.
     * <p>
     * System property: {@code ROOT_RUN_DIR_PROPERTY_NAME}, default "run_cucumber".
     */
    public static final String ROOT_RUN_ROOT_RUN_DIR = "run_cucumber";

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
    public static final String DEFAULT_SERVER_RUN_DIR = "server";

    /**
     * Default subdirectory under root for server artifacts.
     * <p>
     * System property: {@code RUN_DIR_PROPERTY_NAME}, default "server".
     */
    public static final String DEFAULT_CLIENT_RUN_DIR = "client";

    /**
     * By default, server artifacts are not deleted on finish.
     */
    public static final String DEFAULT_DELETE_ON_FINISH = "false";

    /**
     * Default Cucumber engine ID to use in your custom runner.
     */
    public static final String DEFAULT_ENGINE_TO_USE = "cucumber";

    /**
     * Default time-out for FabricEngine set to 10 seconds
     */
    public static final String DEFAULT_ENGINE_TIMEOUT = "40";


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
     * @return root run directory (system property or {@value #ROOT_RUN_ROOT_RUN_DIR})
     */
    public static String getRootRunDir() {
        return getOrDefault(ROOT_RUN_DIR_PROPERTY_NAME, ROOT_RUN_ROOT_RUN_DIR);
    }

    /**
     * @return fabric run envType as string (system property or {@value #DEFAULT_ENVTYPE})
     */
    public static String getEnvTypeString() {
        return getOrDefault(ENVTYPE_PROPERTY_NAME, DEFAULT_ENVTYPE);
    }

    /**
     * @return fabric run envType (system property or {@value #DEFAULT_ENVTYPE})
     */
    public static EnvType getEnvType() {
        return EnvType.valueOf(getOrDefault(ENVTYPE_PROPERTY_NAME, DEFAULT_ENVTYPE).toUpperCase());
    }

    /**
     * @return full server run path: {root} + {server}
     */
    public static Path getServerRunPath() {
        return Paths.get(
                getRootRunDir(),
                getOrDefault(RUN_SERVER_DIR_PROPERTY_NAME, DEFAULT_SERVER_RUN_DIR)
        );
    }

    /**
     * @return server run directory name (system property or {@value #DEFAULT_SERVER_RUN_DIR})
     */
    public static String getServerRunDir() {
        return getOrDefault(RUN_SERVER_DIR_PROPERTY_NAME, DEFAULT_SERVER_RUN_DIR);
    }

    /**
     * @return full client run path: {root} + {client}
     */
    public static Path getClientRunPath() {
        return Paths.get(
                getRootRunDir(),
                getOrDefault(RUN_CLIENT_DIR_PROPERTY_NAME, DEFAULT_CLIENT_RUN_DIR)
        );
    }

    /**
     * @return client run directory name (system property or {@value #DEFAULT_CLIENT_RUN_DIR})
     */
    public static String getClientRunDir() {
        return getOrDefault(RUN_CLIENT_DIR_PROPERTY_NAME, DEFAULT_CLIENT_RUN_DIR);
    }

    /**
     * @return whether to delete on finish (system property or {@value #DEFAULT_DELETE_ON_FINISH})
     */
    public static boolean isDeleteOnFinish() {
        return Boolean.parseBoolean(getOrDefault(DELETE_ON_FINISH_PROPERTY_NAME, DEFAULT_DELETE_ON_FINISH));
    }

    /**
     * @return engine ID to use (system property or {@value #DEFAULT_ENGINE_TO_USE})
     */
    public static String getEngineToUse() {
        return getOrDefault(ENGINE_TO_USE_PROPERTY_NAME, DEFAULT_ENGINE_TO_USE);
    }

    /**
     * @return timeout in seconds (system property or {@value #DEFAULT_ENGINE_TIMEOUT})
     */
    public static String getEngineTimeout() {
        return getOrDefault(ENGINE_TIMEOUT_PROPERTY_NAME, DEFAULT_ENGINE_TIMEOUT);
    }


    // CONVENIENCE METHODS (ConfigurationParameters)

    /**
     * @see #getRootRunDir()
     */
    public static String getRootRunDir(ConfigurationParameters params) {
        return getOrDefault(params,
                ROOT_RUN_DIR_PROPERTY_NAME,
                ROOT_RUN_ROOT_RUN_DIR);
    }

    /**
     * @see #getServerRunDir()
     */
    public static String getServerRunDir(ConfigurationParameters params) {
        return getOrDefault(params,
                RUN_SERVER_DIR_PROPERTY_NAME,
                DEFAULT_SERVER_RUN_DIR);
    }

    /**
     * @see #getClientRunDir()
     */
    public static String getClientRunDir(ConfigurationParameters params) {
        return getOrDefault(params,
                RUN_CLIENT_DIR_PROPERTY_NAME,
                DEFAULT_CLIENT_RUN_DIR);
    }

    /**
     * @see #isDeleteOnFinish()
     */
    public static boolean isDeleteOnFinish(ConfigurationParameters params) {
        return Boolean.parseBoolean(getOrDefault(params,
                DELETE_ON_FINISH_PROPERTY_NAME,
                DEFAULT_DELETE_ON_FINISH));
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
     * @see #getEnvType()
     */
    public static EnvType getEnvType(ConfigurationParameters params) {
        return EnvType.valueOf(getOrDefault(params,
                ENVTYPE_PROPERTY_NAME,
                DEFAULT_ENVTYPE).toUpperCase());
    }

    /**
     * @see #getEnvType()
     */
    public static int getEngineTimeout(ConfigurationParameters params) {
        return Integer.parseInt(getOrDefault(params,
                ENGINE_TIMEOUT_PROPERTY_NAME,
                DEFAULT_ENGINE_TIMEOUT));
    }

    /**
     * Take an existing map of config-string→string, and ensure every
     * Constants.* key is present (using either the provided value
     * or the Constants.DEFAULT_* fallback).
     */
    public static Map<String, String> mergeWithDefaults(Map<String, String> provided) {
        Map<String, String> merged = new LinkedHashMap<>(provided);

        if (!provided.containsKey(ROOT_RUN_DIR_PROPERTY_NAME)) {
            merged.put(ROOT_RUN_DIR_PROPERTY_NAME,
                    getRootRunDir());
        }
        if (!provided.containsKey(ENGINE_TO_USE_PROPERTY_NAME)) {
            merged.put(ENGINE_TO_USE_PROPERTY_NAME,
                    getEngineToUse());
        }

        if (merged.get(ENVTYPE_PROPERTY_NAME).equals("server")) {
            if (!provided.containsKey(RUN_SERVER_DIR_PROPERTY_NAME)) {
                merged.put(RUN_SERVER_DIR_PROPERTY_NAME,
                        getServerRunDir());
            }
        } else if (merged.get(ENVTYPE_PROPERTY_NAME).equals("client")) {
            if (!provided.containsKey(RUN_CLIENT_DIR_PROPERTY_NAME)) {
                merged.put(RUN_CLIENT_DIR_PROPERTY_NAME,
                        getClientRunDir());
            }
        } else {
            throw new IllegalArgumentException("Expected client or server as EngineToUse property, but received " + merged.get(ENVTYPE_PROPERTY_NAME));
        }

        return merged;
    }
}
