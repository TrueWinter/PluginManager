package dev.truewinter.PluginManager;

import org.jetbrains.annotations.NotNull;

/**
 * <p>You should extend this class and all plugins must extend your new Plugin class.
 * @implNote Each method in this class has an implementor tag
 * specifying whose responsible for implementing the method:</p>
 * <ul>
 *   <li>software developer: This is a developer who uses PluginManager in their software</li>
 *   <li>plugin developer: This is a developer who creates plugins for software that uses PluginManager</li>
 * </ul>
 */
@SuppressWarnings("unused")
public abstract class Plugin<T> {
    private String name = null;

    /**
     * @hidden
     */
    private void setName(String name) {
        this.name = name;
    }

    /**
     * @return The plugin's name
     */
    protected final String getName() {
        return name;
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
