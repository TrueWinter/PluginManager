package dev.truewinter.PluginManager;

import dev.dejvokep.boostedyaml.YamlDocument;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URLClassLoader;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * <p>The default PluginManager which requires a <code>plugin.yml</code> file in the resources directory containing the following:</p>
 * <ul>
 *  <li>name: A unique name for this plugin</li>
 *  <li>main_class: The plugin's main class, which must extend {@link Plugin}</li>
 * </ul>
 *
 * {@link AbstractPluginManager} can be extended to create a custom PluginManager.
 */
@SuppressWarnings("unused")
public class PluginManager<T> extends AbstractPluginManager<T> {
    public PluginManager(Consumer<Logger.PluginManagerLog> logger) {
        super(logger);
    }

    @Override
    protected final String getPluginMainClass(@NotNull URLClassLoader plugin) throws IOException {
        return getPluginYaml(plugin).getString("main_class");
    }

    @Override
    protected final String getPluginName(@NotNull URLClassLoader plugin) throws IOException {
        return getPluginYaml(plugin).getString("name");
    }

    /**
     * @hidden
     */
    private YamlDocument getPluginYaml(URLClassLoader plugin) throws IOException {
        return YamlDocument.create(Objects.requireNonNull(plugin.getResourceAsStream("plugin.yml")));
    }
}
