# PluginManager

PluginManager is a library that can be easily integrated with your software to allow your users to create plugins.

## Usage

```xml
<project>
    <repositories>
        <repository>
            <id>jitpack.io</id>
            <url>https://jitpack.io</url>
        </repository>
    </repositories>
    
    <dependencies>
        <dependency>
            <groupId>dev.truewinter</groupId>
            <artifactId>PluginManager</artifactId>
            <version>0.0.1</version>
        </dependency>
    </dependencies>
</project>
```

The default plugin manager requires a `plugin.yml` to be included in the plugin containing the following:

```yaml
# A unique name for this plugin, must be alphanumeric
name: PluginName
# The plugin's main class
main_class: org.example.plugin.Plugin
```

An example for using the plugin manager for software called CoolSoftware, loading a plugin called CoolPlugin, is included below:

```java
// In CoolSoftware
public interface CoolSoftwareAPI {
    // CoolSoftware's API methods
    void exampleApiMethod();
}

public class PluginLoader {
    public PluginLoader() {
        PluginManager<CoolSoftwareAPI> pluginManager = new PluginManager<>(c -> {
            // Handle PluginManagerLog messages
        });
        
        // This variable should contain a list of plugins, including CoolPlugin
        List<File> pluginJars = new ArrayList<>();
        pluginManager.loadPlugins(pluginJars);
    }
}

public abstract class CoolSoftwarePlugin extends Plugin<CoolSoftwareAPI> {
    // Implement methods
}

public class SomeEvent extends Event {
    public String exampleEventMethod() {
        return "SomeEvent example";
    }
}
```

```java
// In CoolPlugin
public class EventListener implements Listener {
    @EventHandler
    public onEvent(SomeEvent e) {
        e.exampleEventMethod();
    }
}

// If a plugin wants to use the getPluginByName(String) method,
// it must implement UsesAnotherPlugin and only call the method
// after the onAllPluginsLoaded() method.
public class CoolPlugin extends CoolSoftwarePlugin {
    // It is only safe to interact with the API once the onLoad()
    // method has been called.
    @Override
    protected void onLoad() {
        getLogger().info("Plugin loaded");
        getApi().exampleApiMethod();
        registerListeners(this, new EventListener());
    }
    
    @Override
    protected void onUnload() {
        getLogger().info("Plugin unloaded");
    }
}
```

## Docs

[Javadoc available on Jitpack](https://javadoc.jitpack.io/com/github/TrueWinter/PluginManager/latest/javadoc/)