package dev.truewinter.PluginManager;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public abstract class Logger {
    private final String pluginName;

    protected Logger(String pluginName) {
        this.pluginName = pluginName;
    }

    protected String getPluginName() {
        return pluginName;
    }

    public abstract void info(String s);
    public abstract void warn(String s);
    public abstract void warn(String s, Throwable t);
    public abstract void error(String s);
    public abstract void error(String s, Throwable t);

    /**
     * PluginManagerLogs are created by the PluginManager
     * to allow you to control what message to show users
     */
    public static class PluginManagerLog {
        private final LogEvents event;
        private final String pluginName;
        private Throwable exception = null;

        protected PluginManagerLog(@NotNull LogEvents event, @NotNull String pluginName) {
            this.event = event;
            this.pluginName = pluginName;
        }

        protected PluginManagerLog(@NotNull LogEvents event, @NotNull String pluginName, @NotNull Throwable e) {
            this.event = event;
            this.pluginName = pluginName;
            this.exception = e;
        }

        public LogEvents getEvent() {
            return event;
        }

        /**
         * @return The plugins name if available, else the {@link Class#getSimpleName()}
         */
        public String getPluginName() {
            return pluginName;
        }

        @Nullable
        public Throwable getException() {
            return exception;
        }
    }

    public enum LogEvents {
        PLUGIN_LOADED,
        PLUGIN_UNLOADED,
        PLUGIN_LOADING_ERROR,
        PLUGIN_UNLOADING_ERROR,
        UNKNOWN_PLUGIN_ERROR,
        ALL_PLUGINS_LOADED_FAILED_CALL_ERROR,
        EVENT_DISPATCH_CALL_ERROR
    }
}
