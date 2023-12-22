package dev.truewinter.PluginManager;

/**
 * If a plugin wants to call the {@link AbstractPluginManager#getPluginByName(Plugin, String)} method,
 * it must implement this interface.
 */
public interface UsesAnotherPlugin {
    /**
     * This method will be called once all plugins are loaded.
     * Only after this time may the {@link AbstractPluginManager#getPluginByName(Plugin, String)}
     * method be called.
     */
    void onAllPluginsLoaded();
}
