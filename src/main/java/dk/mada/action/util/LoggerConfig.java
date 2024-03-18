package dk.mada.action.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Configures (JUL) logging backend.
 */
public final class LoggerConfig {
    private LoggerConfig() {
    }

    /** Loads default logger configuration, sets info level. */
    public static void loadDefaultConfig() {
        loadDefaultConfig(Level.INFO);
    }

    /**
     * Loads default logger configuration and activates desired default level.
     *
     * @param level the logging level to activate
     */
    public static void loadDefaultConfig(Level level) {
        loadConfig("/logging.properties");
        Logger.getLogger("dk.mada").setLevel(level);
    }

    /**
     * Loads specific logger configuration.
     *
     * @param path resource path of logger configuration
     */
    public static void loadConfig(String path) {
        try (InputStream configFile = LoggerConfig.class.getResourceAsStream(path)) {
            LogManager.getLogManager().updateConfiguration(configFile, null);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load logging properties", e);
        }
    }
}
