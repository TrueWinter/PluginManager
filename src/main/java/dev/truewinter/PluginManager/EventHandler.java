package dev.truewinter.PluginManager;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * All event listener methods must be annotated with this, or will be ignored
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface EventHandler {
    /**
     * Setting option to true will allow your listener method to receive cancelled events
     * @return Whether this event is cancelled or not
     */
    boolean receiveCancelled() default false;
}
