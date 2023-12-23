package dev.truewinter.PluginManager;

import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public abstract class AbstractPluginManager<T> {
    private final Consumer<Logger.PluginManagerLog> logger;
    private boolean allPluginsLoaded = false;
    private final ConcurrentHashMap<String, Plugin<T>> plugins = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Class<? extends Event>, LinkedHashMap<Plugin<T>, EventHandlerContainer>> events = new ConcurrentHashMap<>();

    /**
     * Instantiates the PluginManager with a simple logger.
     * This should only be used in development.
     */
    public AbstractPluginManager() {
        this.logger = log -> {
            if (log.getException() != null) {
                System.err.printf("[%s] %s %s%n", log.getPluginName(), log.getEvent().name(), log.getException());
            } else {
                System.out.printf("[%s] %s%n", log.getPluginName(), log.getEvent().name());
            }
        };
    }

    /**
     * Instantiates the PluginManager with a custom logger
     * @param logger Logger
     */
    public AbstractPluginManager(Consumer<Logger.PluginManagerLog> logger) {
        this.logger = logger;
    }

    /**
     * Loads the plugins
     * @param pluginJars A list of JAR {@link File}s
     */
    @MustBeInvokedByOverriders
    public synchronized void loadPlugins(@NotNull List<File> pluginJars) {
        for (File file : pluginJars) {
            Plugin<T> thisPlugin = null;
            try {
                URLClassLoader plugin = new URLClassLoader(
                        new URL[]{file.toURI().toURL()},
                        getClass().getClassLoader()
                );

                String mainClass = getPluginMainClass(plugin);
                String name = getPluginName(plugin);

                Class<? extends Plugin<T>> pluginClass = getPluginAsSubclass(plugin, mainClass);
                Plugin<T> pluginInstance = pluginClass.getDeclaredConstructor().newInstance();
                // thisPlugin is simply here so that if there's an exception past this point,
                // the exception handler will know what plugin caused it.
                thisPlugin = pluginInstance;

                invoke(pluginClass, pluginInstance, name,
                        new MethodConfig("setName", String.class), name);

                invoke(pluginClass, pluginInstance, name,
                        new MethodConfig("setDirectory", String.class), file.getParent());

                URL defaultConfigUrl = plugin.getResource(pluginInstance.getConfigFileName());
                if (defaultConfigUrl != null) {
                    try (InputStream is = defaultConfigUrl.openStream()) {
                        invoke(pluginClass, pluginInstance, name,
                                new MethodConfig("setDefaultConfig", String.class),
                                IOUtils.toString(is, StandardCharsets.UTF_8));
                    }
                }

                plugins.put(name, pluginInstance);

                Method onLoadMethod = pluginClass.getDeclaredMethod("onLoad");
                onLoadMethod.setAccessible(true);
                onLoadMethod.invoke(pluginInstance);

                logger.accept(new Logger.PluginManagerLog(Logger.LogEvents.PLUGIN_LOADED, name));
                plugin.close();
            } catch (Exception e) {
                logger.accept(new Logger.PluginManagerLog(Logger.LogEvents.PLUGIN_LOADING_ERROR, file.getName(), e));

                if (thisPlugin != null) {
                    if (thisPlugin.getName() != null) {
                        plugins.remove(thisPlugin.getName());
                    }

                    try {
                        Method onUnloadMethod = thisPlugin.getClass().getDeclaredMethod("onUnload");
                        onUnloadMethod.setAccessible(true);
                        onUnloadMethod.invoke(thisPlugin);
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        }

        allPluginsLoaded = true;
        plugins.forEach((name, plugin) -> {
            if (doesPluginImplementUsesAnotherPlugin(plugin)) {
                try {
                    Method onAllPluginsLoadedMethod = plugin.getClass().getDeclaredMethod("onAllPluginsLoaded");
                    onAllPluginsLoadedMethod.setAccessible(true);
                    onAllPluginsLoadedMethod.invoke(plugin);
                } catch (Exception e) {
                    logger.accept(new Logger.PluginManagerLog(Logger.LogEvents.ALL_PLUGINS_LOADED_FAILED_CALL_ERROR, name, e));
                }
            }
        });
    }

    @SuppressWarnings("rawtypes")
    private record MethodConfig(String method, Class... types) {}

    @SuppressWarnings({"rawtypes", "BooleanMethodIsAlwaysInverted"})
    private void invoke(Class pluginClass, Plugin<T> pluginInstance, String name,
                           MethodConfig methodConfig, Object... param) throws Exception {
        Method methodToCall = traverseSuperClassesForMethod(pluginClass, methodConfig.method(), methodConfig.types());
        if (methodToCall == null) {
            throw new Exception("Method " + methodConfig.method() + " missing from class");
        }
        methodToCall.setAccessible(true);
        methodToCall.invoke(pluginInstance, param);
    }

    @Nullable
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Method traverseSuperClassesForMethod(Class pluginClass, String method, Class... types) {
        try {
            return pluginClass.getDeclaredMethod(method, types);
        } catch (NoSuchMethodException e) {
            Class superClass = pluginClass.getSuperclass();
            if (superClass != null) {
                return traverseSuperClassesForMethod(superClass, method, types);
            }

            return null;
        }
    }

    /**
     * Unloads a plugin and unregisters all events it registered
     * @param plugin The plugin
     */
    @MustBeInvokedByOverriders
    public synchronized void unloadPlugin(@NotNull Plugin<T> plugin) {
        String name = plugin.getName();
        if (name == null) {
            logger.accept(new Logger.PluginManagerLog(Logger.LogEvents.UNKNOWN_PLUGIN_ERROR, plugin.getClass().getSimpleName()));
            return;
        }

        plugins.remove(name);

        events.forEach((eventClass, listeners) -> events.get(eventClass).remove(plugin));

        try {
            Method onUnloadMethod = plugin.getClass().getDeclaredMethod("onUnload");
            onUnloadMethod.setAccessible(true);
            onUnloadMethod.invoke(plugin);
        } catch (Exception e) {
            logger.accept(new Logger.PluginManagerLog(Logger.LogEvents.PLUGIN_UNLOADING_ERROR, name, e));
        }

        logger.accept(new Logger.PluginManagerLog(Logger.LogEvents.PLUGIN_UNLOADED, name));
    }

    /**
     * Registers an event listener for a plugin
     * @param plugin The plugin
     * @param listener The event listener
     * @throws Exception if PluginManager fails to register the listener
     */
    @MustBeInvokedByOverriders
    public synchronized void registerListener(@NotNull Plugin<T> plugin, @NotNull Listener listener) throws Exception {
        String name = plugin.getName();
        if (name == null) {
            logger.accept(new Logger.PluginManagerLog(Logger.LogEvents.UNKNOWN_PLUGIN_ERROR, plugin.getClass().getSimpleName()));
            return;
        }

        if (!plugins.contains(plugin)) {
            throw new Exception("Failed to register listeners for plugin \"" + name + "\". Plugin not loaded.");
        }

        for (Method method : listener.getClass().getDeclaredMethods()) {
            method.setAccessible(true);
            if (method.isAnnotationPresent(EventHandler.class)) {
                if (method.getParameterCount() != 1) {
                    throw new Exception("Failed to load listener method \"" + method.getName() + "\" in plugin \"" + name + "\". Event handler methods must contain only one parameter.");
                }

                try {
                    Class<?> firstParam = method.getParameterTypes()[0];
                    // Verify it extends Event
                    firstParam.asSubclass(Event.class);

                    if (!events.containsKey(firstParam)) {
                        events.put(firstParam.asSubclass(Event.class), new LinkedHashMap<>());
                    }

                    events.get(firstParam.asSubclass(Event.class)).put(plugin, new EventHandlerContainer(listener, method));
                } catch (Exception e) {
                    throw new Exception("Failed to load listener method \"" + method.getName() + "\" in plugin \"" + name + "\":", e);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Plugin<T>> getPluginAsSubclass(@NotNull URLClassLoader plugin, @NotNull String mainClass) throws Exception {
        try {
            return (Class<? extends Plugin<T>>) Class.forName(mainClass, false, plugin).asSubclass(Plugin.class);
        } catch (ClassCastException e) {
            throw new Exception("Plugin does not extend Plugin class");
        }
    }

    private boolean doesPluginImplementUsesAnotherPlugin(@NotNull Plugin<T> plugin) {
        try {
            return UsesAnotherPlugin.class.isAssignableFrom(plugin.getClass());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Fires an event to all listeners registered for the event
     * @param event The event
     * @return The event, after all listeners have received it
     * @param <E> The event class
     */
    public synchronized <E extends Event> E fireEvent(@NotNull E event) {
        if (!events.containsKey(event.getClass())) return event;

        events.get(event.getClass()).forEach((plugin, eventHandler) -> {
            String name = plugin.getName();
            if (!(event.isCancelled() && !eventHandler.method().getAnnotation(EventHandler.class).receiveCancelled())) {
                try {
                    eventHandler.method().invoke(eventHandler.instance(), event);
                } catch (Exception e) {
                    logger.accept(new Logger.PluginManagerLog(
                            Logger.LogEvents.EVENT_DISPATCH_CALL_ERROR,
                            Objects.requireNonNullElseGet(name, () -> plugin.getClass().getSimpleName()),
                            e
                    ));
                }
            }
        });

        return event;
    }

    /**
     * @see Plugin#getPluginByName(String)
     */
    @Nullable
    public synchronized Plugin<T> getPluginByName(@NotNull Plugin<T> requestingPlugin, @NotNull String name) throws ClassCastException, IllegalStateException {
        if (!doesPluginImplementUsesAnotherPlugin(requestingPlugin)) {
            throw new ClassCastException("Plugin must implement UsesAnotherPlugin to use the getPluginByName method.");
        }

        if (!allPluginsLoaded) {
            throw new IllegalStateException("The getPluginByName method can only be called after all plugins have been loaded.");
        }

        if (!plugins.containsKey(name)) {
            return null;
        }

        return plugins.get(name);
    }

    /**
     * Unloads all plugins
     */
    public synchronized void handleShutdown() {
        plugins.forEach((name, plugin) -> unloadPlugin(plugin));
    }

    private record EventHandlerContainer(Listener instance, Method method) {}

    /**
     * @param plugin A URLClassLoader
     * @return The plugin's main class
     */
    protected abstract String getPluginMainClass(@NotNull URLClassLoader plugin) throws IOException;

    /**
     * @param plugin A URLClassLoader
     * @return The plugin's name
     */
    protected abstract String getPluginName(@NotNull URLClassLoader plugin) throws IOException;
}
