package net.cucumberfabric.options;

import org.junit.platform.engine.ConfigurationParameters;

import java.nio.file.Path;
import java.nio.file.Paths;

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
     * Property name to set the server-only run directory.
     * <p>
     * Accepts a file-system path.
     * Default: {@value #SERVER_DEFAULT_RUN_DIR}
     * <br>
     * Note: This folder will be created (inside {@value #ROOT_DEFAULT_RUN_DIR}) if it doesn't exist.
     */
    public static final String SERVER_RUN_DIR_PROPERTY_NAME =
            "cucumberfabric.server-run-dir";

    /**
     * Property name to set the client-only run directory.
     * <p>
     * Accepts a file-system path.
     * Default: {@value #CLIENT_DEFAULT_RUN_DIR}
     * <br>
     * Note: This folder will be created (inside {@value #ROOT_DEFAULT_RUN_DIR}) if it doesn't exist.
     */
    public static final String CLIENT_RUN_DIR_PROPERTY_NAME =
            "cucumberfabric.client-run-dir";

    /**
     * Property name to control whether the server state is saved on stop.
     * <p>
     * Accepts "true" or "false".
     * Default: {@value #DEFAULT_SAVE_ON_STOP}
     */
    public static final String SAVE_ON_STOP_PROPERTY_NAME =
            "cucumberfabric.save-on-stop";

    /**
     * Property name to control deletion of data when the server finishes.
     * <p>
     * Accepts "true" or "false".
     * Default: {@value #DEFAULT_DELETE_ON_FINISH}
     */
    public static final String DELETE_ON_FINISH_PROPERTY_NAME =
            "cucumberfabric.delete-on-finish";


    // DEFAULT VALUES

    /**
     * Root directory which is used to run all cucumber artifacts.
     * <p>
     * System property: {@code ROOT_RUN_DIR_PROPERTY_NAME}, default "run_cucumber".
     */
    public static final String ROOT_DEFAULT_RUN_DIR = "run_cucumber";

    /**
     * Default subdirectory under root for server artifacts.
     * <p>
     * System property: {@code SERVER_RUN_DIR_PROPERTY_NAME}, default "server".
     */
    public static final String SERVER_DEFAULT_RUN_DIR = "server";

    /**
     * Default subdirectory under root for client artifacts.
     * <p>
     * System property: {@code CLIENT_RUN_DIR_PROPERTY_NAME}, default "client".
     */
    public static final String CLIENT_DEFAULT_RUN_DIR = "client";

    /**
     * By default, server state will be persisted when stopped.
     */
    public static final boolean DEFAULT_SAVE_ON_STOP = true;

    /**
     * By default, server artifacts are not deleted on finish.
     */
    public static final boolean DEFAULT_DELETE_ON_FINISH = false;


    // GENERIC System.getProperty–BASED getters

    /**
     * Returns the system-property value if set and non-blank,
     * otherwise returns the supplied default.
     */
    public static String getOrDefault(String propertyName,
                                      String defaultValue) {
        String v = System.getProperty(propertyName);
        return (v != null && !v.isBlank()) ? v : defaultValue;
    }

    /**
     * Returns the system-property boolean value if set and non-blank,
     * otherwise returns the supplied default.
     */
    public static boolean getOrDefault(String propertyName,
                                       boolean defaultValue) {
        String v = System.getProperty(propertyName);
        return (v != null && !v.isBlank())
                ? Boolean.parseBoolean(v)
                : defaultValue;
    }


    // GENERIC ConfigurationParameters–BASED getters

    /**
     * Returns the JUnit ConfigurationParameters value if present and non-blank,
     * otherwise returns the supplied default.
     */
    public static String getOrDefault(ConfigurationParameters params,
                                      String propertyName,
                                      String defaultValue) {
        return params.get(propertyName)
                .filter(s -> !s.isBlank())
                .orElse(defaultValue);
    }

    /**
     * Returns the JUnit ConfigurationParameters boolean value if present and non-blank,
     * otherwise returns the supplied default.
     */
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
     * @return full server run path: {root} + {server}
     */
    public static Path getServerRunPath() {
        String root = getRootRunDir();
        String server = getOrDefault(SERVER_RUN_DIR_PROPERTY_NAME, SERVER_DEFAULT_RUN_DIR);
        return Paths.get(root, server);
    }

    /**
     * @return server run directory (system property or {@value #SERVER_DEFAULT_RUN_DIR})
     */
    public static String getServerRunDir() {
        return getOrDefault(SERVER_RUN_DIR_PROPERTY_NAME, SERVER_DEFAULT_RUN_DIR);
    }

    /**
     * @return full client run path: {root} + {client}
     */
    public static Path getClientRunPath() {
        String root = getRootRunDir();
        String client = getOrDefault(CLIENT_RUN_DIR_PROPERTY_NAME, CLIENT_DEFAULT_RUN_DIR);
        return Paths.get(root, client);
    }

    /**
     * @return client run directory (system property or {@value #CLIENT_DEFAULT_RUN_DIR})
     */
    public static String getClientRunDir() {
        return getOrDefault(CLIENT_RUN_DIR_PROPERTY_NAME, CLIENT_DEFAULT_RUN_DIR);
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
     * @see #getServerRunDir()
     */
    public static String getServerRunDir(ConfigurationParameters params) {
        return getOrDefault(params,
                SERVER_RUN_DIR_PROPERTY_NAME,
                SERVER_DEFAULT_RUN_DIR);
    }

    /**
     * @see #getClientRunDir()
     */
    public static String getClientRunDir(ConfigurationParameters params) {
        return getOrDefault(params,
                CLIENT_RUN_DIR_PROPERTY_NAME,
                CLIENT_DEFAULT_RUN_DIR);
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
}
