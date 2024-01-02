package dev.truewinter.PluginManager;

import dev.dejvokep.boostedyaml.YamlDocument;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URLClassLoader;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.jar.JarFile;

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
    public PluginManager(ClassLoader classLoader, Consumer<Logger.PluginManagerLog> logger) {
        super(classLoader, logger);
    }

    @Override
    protected final String getPluginMainClass(@NotNull JarFile jarFile) throws IOException {
        return getPluginYaml(jarFile).getString("main_class");
    }

    @Override
    protected final String getPluginName(@NotNull JarFile jarFile) throws IOException {
        return getPluginYaml(jarFile).getString("name");
    }

    private YamlDocument getPluginYaml(JarFile jarFile) throws IOException {
        return YamlDocument.create(Objects.requireNonNull(jarFile.getInputStream(jarFile.getEntry("plugin.yml"))));
    }
}
