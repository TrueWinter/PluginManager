package dev.truewinter.PluginManager;

public class CancellableEvent extends Event {
    private boolean cancelled = false;

    /**
     * @return whether this event was cancelled
     */
    @Override
    public final boolean isCancelled() {
        return cancelled;
    }

    /**
     * This method allows you to cancel events so other plugins' {@link EventHandler}s do not receive them.
     * Note that any {@link EventHandler} with {@link EventHandler#receiveCancelled()} set to true will still
     * receive the events.
     * @param cancelled A boolean specifying the cancellation status
     */
    @SuppressWarnings("unused")
    public final void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
