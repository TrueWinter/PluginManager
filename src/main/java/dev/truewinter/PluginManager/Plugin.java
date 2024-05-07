package dev.truewinter.PluginManager;

import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * <p>You should extend this class and all plugins must extend your new Plugin class.
 * @implNote Each method in this class has an implementor tag
 * specifying whose responsible for implementing the method:</p>
 * <ul>
 *   <li>software developer: This is a developer who uses PluginManager in their software</li>
 *   <li>plugin developer: This is a developer who creates plugins for software that uses PluginManager</li>
 * </ul>
 * <p><strong>Important: Do not call any methods, with the exception of
 * ensureNoApiInteractionInConstructor(), before the onLoad() method has
 * been called. Doing so will result in unexpected results.</strong></p>
 */
@SuppressWarnings("unused")
public abstract class Plugin<T> {
    private String name = null;
    private String directory = null;
    private String defaultConfig = null;

    protected final void ensureNoApiInteractionInConstructor() {
        if (getName() == null) {
            throw new RuntimeException("Attempting to interact with the API before the onLoad() method is called is not allowed");
        }
    }

    private void setName(String name) {
        this.name = name;
    }

    private void setDirectory(String directory) {
        this.directory = directory;
    }

    private void setDefaultConfig(String defaultConfig) {
        this.defaultConfig = defaultConfig;
    }

    /**
     * @return The plugin's name
     */
    public final String getName() {
        // This method is used internally before the plugin is loaded,
        // so don't call ensureNoApiInteractionInConstructor() here
        return name;
    }

    /**
     * Returns the name of the config file, defaulting
     * to <code>config.yml</code>. This should be the
     * same name as the config file in the JAR file,
     * and will be used by other configuration related
     * methods.
     * @implementor software developer
     */
    protected String getConfigFileName() {
        return "config.yml";
    }

    /**
     * Returns the relative location of the configuration
     * file on disk,
     * defaulting to <code>{name}/{configFileName}</code>
     * @implementor software developer
     */
    protected String getConfigFileDiskLocation() {
        ensureNoApiInteractionInConstructor();
        return name + "/" + getConfigFileName();
    }

    /**
     * Returns a {@link File} referencing the absolute
     * path of the configuration file.
     */
    protected final File getConfig() {
        ensureNoApiInteractionInConstructor();
        return new File(directory, getConfigFileDiskLocation());
    }

    /**
     * Copies the default configuration, if it exists, from the plugin
     * to the File returned by {@link Plugin#getConfig()}. If the file
     * already exists, it will not be overwritten.
     * @throws IOException if copying the default configuration failed
     * @throws NullPointerException if the default configuration does not exist
     */
    protected final void copyDefaultConfig() throws IOException, NullPointerException {
        if (defaultConfig == null) {
            throw new NullPointerException();
        }

        if (getConfig().exists()) return;

        if (!getConfig().getParentFile().exists() && !getConfig().getParentFile().mkdirs()) {
            throw new IOException("Failed to create parent directory");
        }

        try (OutputStream outputStream = new FileOutputStream(getConfig())) {
            IOUtils.write(defaultConfig, outputStream, StandardCharsets.UTF_8);
        }
    }

    /**
     * This method is called when the plugin is loaded. It is only safe to interact with the software after this is called.
     * @see UsesAnotherPlugin#onAllPluginsLoaded()
     * @implementor plugin developer
     */
    protected abstract void onLoad();

    /**
     * This method is called when the plugin is unloaded. This will happen when software is shut down,
     * or when an exception is thrown while the plugin is loading, registering listeners, or incorrectly using
     * the {@link AbstractPluginManager#getPluginByName(Plugin, String)} method.
     * @implementor plugin developer
     */
    protected abstract void onUnload();

    /**
     * Use this method to register event listeners
     * @param plugin An instance of the plugin
     * @param listener An instance of the event listener class
     * @implementor software developer, calling {@link AbstractPluginManager#registerListener(Plugin, Listener)}
     */
    protected abstract void registerListeners(Plugin<T> plugin, Listener listener) throws Exception;

    /**
     * @return the logger
     * @implementor software developer
     */
    protected abstract Logger getLogger();

    /**
     * This method allows plugin developers to get an instance of another plugin installed on the
     * same instance of the software. Using this method is only allowed after the
     * {@link UsesAnotherPlugin#onAllPluginsLoaded()} method has been called. The requesting plugin
     * (the one calling this method) must implement {@link UsesAnotherPlugin} to use this.
     * @param name The name of the plugin to get, as set in the plugin's plugin.yml
     * @return The plugin, or null if it isn't loaded
     * @throws ClassCastException if the requesting plugin does not implement {@link UsesAnotherPlugin}
     * @throws IllegalStateException if this method is called before all plugins have loaded
     * @implementor software developer, calling {@link AbstractPluginManager#getPluginByName(Plugin, String)}
     */
    protected abstract Plugin<T> getPluginByName(@NotNull String name) throws ClassCastException, IllegalStateException;

    /**
     * @return An instance of the API
     * @implementor software developer
     */
    protected abstract T getApi();
}
