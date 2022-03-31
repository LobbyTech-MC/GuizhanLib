package net.guizhanss.guizhanlib.slimefun.addon;

import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;
import io.github.thebusybiscuit.slimefun4.libraries.dough.config.Config;
import lombok.SneakyThrows;
import net.guizhanss.guizhanlib.updater.GuizhanBuildsUpdater;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;
import java.text.MessageFormat;
import java.util.Objects;

/**
 * An abstract {@link SlimefunAddon} class that contains
 * the updater and some utilities.
 *
 * Extend this as your main class to use them.
 *
 * This is modified from InfinityLib
 *
 * @author Mooy1
 * @author ybw0014
 */
@ParametersAreNonnullByDefault
public abstract class AbstractAddon extends JavaPlugin implements SlimefunAddon {

    private static AbstractAddon instance;

    private final GuizhanBuildsUpdater updater;
    private final Environment environment;
    private final String githubUser;
    private final String githubRepo;
    private final String githubBranch;
    private final String autoUpdateKey;
    private final String bugTrackerURL;

    private Config config;
    private boolean loading;
    private boolean enabling;
    private boolean disabling;
    private boolean autoUpdateEnabled;

    /**
     * Live addon constructor
     *
     * @param githubUser GitHub username of this project
     * @param githubRepo GitHub repository of this project
     * @param githubBranch GitHub branch of this project
     * @param autoUpdateKey Auto update key in the config
     */
    public AbstractAddon(String githubUser, String githubRepo, String githubBranch, String autoUpdateKey) {
        this.environment = Environment.LIVE;
        this.githubUser = githubUser;
        this.githubRepo = githubRepo;
        this.githubBranch = githubBranch;
        this.autoUpdateKey = autoUpdateKey;
        this.updater = new GuizhanBuildsUpdater(this, getFile(), githubUser, githubRepo, githubBranch, false);
        this.bugTrackerURL = MessageFormat.format("https://github.com/{0}/{1}/issues", githubUser, githubRepo);
        validate();
    }


    /**
     * Testing addon Constructor
     */
    public AbstractAddon(JavaPluginLoader loader, PluginDescriptionFile description, File dataFolder, File file,
                         String githubUser, String githubRepo, String githubBranch, String autoUpdateKey) {
        this(loader, description, dataFolder, file, githubUser, githubRepo, githubBranch, autoUpdateKey, Environment.TESTING);
    }

    /**
     * Testing library Constructor
     */
    AbstractAddon(JavaPluginLoader loader, PluginDescriptionFile description, File dataFolder, File file,
                  String githubUser, String githubRepo, String githubBranch, String autoUpdateKey,
                  Environment environment) {
        super(loader, description, dataFolder, file);
        this.updater = null;
        this.environment = environment;
        this.githubUser = githubUser;
        this.githubBranch = githubBranch;
        this.githubRepo = githubRepo;
        this.autoUpdateKey = autoUpdateKey;
        this.bugTrackerURL = MessageFormat.format("https://github.com/{0}/{1}/issues", githubUser, githubRepo);
        validate();
    }

    /**
     * Validate the information given by constructor
     */
    private void validate() {
        if (instance != null) {
            throw new IllegalStateException("Addon " + instance.getName() + " is already using this GuizhanLib, Shade an relocate your own!");
        }
        if (!githubUser.matches("[\\w-]+")) {
            throw new IllegalArgumentException("Invalid githubUser");
        }
        if (!githubRepo.matches("[\\w-]+")) {
            throw new IllegalArgumentException("Invalid githubRepo");
        }
        if (!githubBranch.matches("[\\w-]+")) {
            throw new IllegalArgumentException("Invalid githubBranch");
        }
    }

    /**
     * Use load() instead
     */
    @Override
    public final void onLoad() {
        if (loading) {
            throw new IllegalStateException(getName() + " is already loading! Do not call super.onLoad()!");
        }

        loading = true;

        // Load
        try {
            load();
        }
        catch (RuntimeException e) {
            handleException(e);
        }
        finally {
            loading = false;
        }
    }

    /**
     * Use enable() instead
     */
    @Override
    public final void onEnable() {
        if (enabling) {
            throw new IllegalStateException(getName() + " is already enabling! Do not call super.onEnable()!");
        }

        enabling = true;

        // Set instance
        instance = this;

        // This is used to check if the auto update config is broken
        boolean brokenConfig = false;

        // Check config.yml
        try {
            config = new Config(this, "config.yml");
        } catch (RuntimeException e) {
            brokenConfig = true;
            e.printStackTrace();
        }

        // Validate autoUpdateKey
        if (autoUpdateKey == null || autoUpdateKey.isEmpty()) {
            brokenConfig = true;
            handleException(new IllegalStateException("Invalid autoUpdateKey"));
        }

        // Check updater
        if (updater != null) {
            // Set up warning
            if (brokenConfig) {
                handleException(new IllegalArgumentException("Auto update is not configured correctly"));
            } else {
                autoUpdateEnabled = true;
                updater.start();
            }
        }

        // Call enable()
        try {
            enable();
        } catch (RuntimeException ex) {
            handleException(ex);
        } finally {
            enabling = false;
        }
    }

    /**
     * Use disable() instead.
     */
    @Override
    public final void onDisable() {
        if (disabling) {
            throw new IllegalStateException(getName() + " is already disabling! Do not call super.onDisable()!");
        }

        disabling = true;

        try {
            disable();
        } catch (RuntimeException e) {
            handleException(e);
        } finally {
            disabling = false;
            instance = null;
            config = null;
        }
    }

    /**
     * Called when loading
     */
    protected void load() {

    }

    /**
     * Called when enabling
     */
    protected abstract void enable();

    /**
     * Called when disabling
     */
    protected abstract void disable();

    /**
     * Handle the {@link RuntimeException} in different environments.
     * Print the exception if in live environment, throw the exception if in testing environment
     *
     * @param ex the {@link RuntimeException}
     */
    private void handleException(RuntimeException ex) {
        switch (environment) {
            case LIVE:
                ex.printStackTrace();
                break;
            case TESTING:
                throw ex;
        }
    }

    /**
     * Get an instance of extended class of {@link AbstractAddon}
     *
     * @param <T> The class that extends {@link AbstractAddon}, which is the real addon main class
     *
     * @return The instance of extended class of {@link AbstractAddon}
     */
    @Nonnull
    @SuppressWarnings("unchecked")
    public static <T extends AbstractAddon> T getInstance() {
        return (T) Objects.requireNonNull(instance, "Addon is not enabled!");
    }

    /**
     * Used for Slimefun to get instance of this {@link JavaPlugin}
     *
     * @return the instance of this {@link JavaPlugin}
     */
    @Nonnull
    @Override
    public final JavaPlugin getJavaPlugin() {
        return this;
    }

    /**
     * This returns the default bug tracker URL by
     * the given GitHub username and repository in constructor.
     *
     * Override it if you don't use GitHub issues as bug tracker
     *
     * @return the default bug tracker url
     */
    @Nonnull
    @Override
    public String getBugTrackerURL() {
        return bugTrackerURL;
    }

    /**
     * Get the current {@link Environment}
     *
     * @return the current {@link Environment}
     */
    @Nonnull
    public Environment getEnvironment() {
        return environment;
    }

    /**
     * If the auto update is enabled
     *
     * @return if the auto update is enabled
     */
    public final boolean isAutoUpdateEnabled() {
        return autoUpdateEnabled;
    }

    /**
     * This method should not be called in {@link AbstractAddon}.
     * Call {@link #getAddonConfig()} instead.
     */
    @Override
    @Nonnull
    @SneakyThrows
    public final FileConfiguration getConfig() {
        throw new IllegalAccessException("Call #getAddonConfig() instead to get Config file");
    }

    /**
     * Get the {@link Config} of the addon
     *
     * @return the {@link Config} of the addon
     */
    @Nonnull
    public final Config getAddonConfig() {
        return getInstance().config;
    }

    /**
     * Reload the {@link Config}
     */
    public final void reloadConfig() {
        getAddonConfig().reload();
    }

    /**
     * Save the {@link Config}
     */
    public final void saveConfig() {
        getAddonConfig().save();
    }

    /**
     * Save default config.
     * Overridden and does nothing since it is handled in #onEnable()
     */
    @Override
    public final void saveDefaultConfig() {
    }

    /**
     * Creates a {@link NamespacedKey} from the given string
     *
     * @param key the {@link String} representation of the key
     *
     * @return the {@link NamespacedKey} created from given string
     */
    @Nonnull
    public static NamespacedKey createKey(String key) {
        return new NamespacedKey(getInstance(), key);
    }
}
