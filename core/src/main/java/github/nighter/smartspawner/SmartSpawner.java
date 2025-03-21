package github.nighter.smartspawner;

import github.nighter.smartspawner.api.SmartSpawnerAPI;
import github.nighter.smartspawner.api.SmartSpawnerPlugin;
import github.nighter.smartspawner.api.SmartSpawnerAPIImpl;
import github.nighter.smartspawner.bstats.Metrics;
import github.nighter.smartspawner.commands.CommandHandler;
import github.nighter.smartspawner.commands.list.SpawnerListGUI;
import github.nighter.smartspawner.extras.HopperHandler;
import github.nighter.smartspawner.hooks.protections.api.Lands;
import github.nighter.smartspawner.hooks.shops.IShopIntegration;
import github.nighter.smartspawner.hooks.shops.SaleLogger;
import github.nighter.smartspawner.hooks.shops.ShopIntegrationManager;
import github.nighter.smartspawner.hooks.shops.api.shopguiplus.SpawnerHook;
import github.nighter.smartspawner.hooks.shops.api.shopguiplus.SpawnerProvider;
import github.nighter.smartspawner.migration.SpawnerDataMigration;
import github.nighter.smartspawner.spawner.gui.main.SpawnerMenuAction;
import github.nighter.smartspawner.spawner.gui.main.SpawnerMenuUI;
import github.nighter.smartspawner.spawner.gui.stacker.SpawnerStackerHandler;
import github.nighter.smartspawner.spawner.gui.synchronization.SpawnerGuiViewManager;
import github.nighter.smartspawner.spawner.gui.stacker.SpawnerStackerUI;
import github.nighter.smartspawner.spawner.gui.storage.SpawnerStorageUI;
import github.nighter.smartspawner.spawner.gui.storage.SpawnerStorageAction;
import github.nighter.smartspawner.spawner.interactions.SpawnerClickManager;
import github.nighter.smartspawner.spawner.interactions.destroy.SpawnerBreakListener;
import github.nighter.smartspawner.spawner.interactions.destroy.SpawnerExplosionListener;
import github.nighter.smartspawner.spawner.interactions.place.SpawnerPlaceListener;
import github.nighter.smartspawner.spawner.interactions.stack.SpawnerStackHandler;
import github.nighter.smartspawner.spawner.interactions.type.SpawnEggHandler;
import github.nighter.smartspawner.spawner.lootgen.SpawnerRangeChecker;
import github.nighter.smartspawner.spawner.properties.SpawnerManager;
import github.nighter.smartspawner.spawner.utils.SpawnerMobHeadTexture;
import github.nighter.smartspawner.spawner.lootgen.SpawnerLootGenerator;
import github.nighter.smartspawner.config.ConfigManager;
import github.nighter.smartspawner.language.LanguageManager;
import github.nighter.smartspawner.utils.UpdateChecker;
import github.nighter.smartspawner.nms.VersionInitializer;

import lombok.Getter;
import lombok.experimental.Accessors;

import me.ryanhamshire.GriefPrevention.GriefPrevention;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

@Getter
@Accessors(chain = false)
public class SmartSpawner extends JavaPlugin implements SmartSpawnerPlugin {
    @Getter
    private static SmartSpawner instance;

    // Core UI components
    private SpawnerMenuUI spawnerMenuUI;
    private SpawnerStorageUI spawnerStorageUI;
    private SpawnerStackerUI spawnerStackerUI;

    // Core handlers
    private SpawnEggHandler spawnEggHandler;
    private SpawnerClickManager spawnerClickManager;
    private SpawnerStackHandler spawnerStackHandler;

    // UI actions
    private SpawnerMenuAction spawnerMenuAction;
    private SpawnerStackerHandler spawnerStackerHandler;
    private SpawnerStorageAction spawnerStorageAction;

    // Core managers
    private ConfigManager configManager;
    private LanguageManager languageManager;
    private SpawnerManager spawnerManager;
    private ShopIntegrationManager shopIntegrationManager;
    private HopperHandler hopperHandler;

    // Event handlers and utilities
    private GlobalEventHandlers globalEventHandlers;
    private SpawnerLootGenerator spawnerLootGenerator;
    private SpawnerListGUI spawnerListGUI;
    private SpawnerRangeChecker rangeChecker;
    private SpawnerGuiViewManager spawnerGuiViewManager;
    private SpawnerExplosionListener spawnerExplosionListener;
    private SpawnerBreakListener spawnerBreakListener;
    private SpawnerPlaceListener spawnerPlaceListener;
    private UpdateChecker updateChecker;

    // Integration flags - static for quick access
    public static boolean hasTowny = false;
    public static boolean hasLands = false;
    public static boolean hasWorldGuard = false;
    public static boolean hasGriefPrevention = false;
    public static boolean hasSuperiorSkyblock2 = false;

    // API implementation
    private SmartSpawnerAPIImpl apiImpl;

    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();
        instance = this;

        // Initialize version-specific components
        initializeVersionComponents();

        // Check for data migration needs
        migrateDataIfNeeded();

        // Initialize core components with Scheduler
        initializeComponents().thenRun(() -> {
            Scheduler.runTask(() -> {
                setupCommand();
                checkDependencies();
                setupBtatsMetrics();
                registerListeners();
                initializeSaleLogging();

                long loadTime = System.currentTimeMillis() - startTime;
                getLogger().info("SmartSpawner has been enabled! (Loaded in " + loadTime + "ms)");
            });
        });
    }

    @Override
    public SmartSpawnerAPI getAPI() {
        return apiImpl;
    }

    private void initializeVersionComponents() {
        try {
            new VersionInitializer(this).initialize();
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize version-specific components", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void migrateDataIfNeeded() {
        SpawnerDataMigration migration = new SpawnerDataMigration(this);
        if (migration.checkAndMigrateData()) {
            getLogger().info("Data migration completed. Loading with new format...");
        }
    }

    private CompletableFuture<Void> initializeComponents() {
        // Initialize core components in order
        this.configManager = new ConfigManager(this);
        this.languageManager = new LanguageManager(this);
        this.spawnerStorageUI = new SpawnerStorageUI(this);
        this.spawnerManager = new SpawnerManager(this);
        this.spawnerListGUI = new SpawnerListGUI(this);
        this.spawnerGuiViewManager = new SpawnerGuiViewManager(this);
        this.spawnerLootGenerator = new SpawnerLootGenerator(this);
        this.rangeChecker = new SpawnerRangeChecker(this);

        // Parallel initialization for components that can be initialized concurrently
        CompletableFuture<Void> asyncInit = Scheduler.supplyAsync(() -> {
            this.shopIntegrationManager = new ShopIntegrationManager(this);
            this.updateChecker = new UpdateChecker(this, "9tQwxSFr");
            return null;
        });

        // Main thread initialization for components that need the main thread
        this.spawnerMenuUI = new SpawnerMenuUI(this);
        this.spawnerStackerUI = new SpawnerStackerUI(this);

        this.spawnEggHandler = new SpawnEggHandler(this);
        this.spawnerStackHandler = new SpawnerStackHandler(this);
        this.spawnerClickManager = new SpawnerClickManager(this);

        this.spawnerMenuAction = new SpawnerMenuAction(this);
        this.spawnerStackerHandler = new SpawnerStackerHandler(this);
        this.spawnerStorageAction = new SpawnerStorageAction(this);

        this.globalEventHandlers = new GlobalEventHandlers(this);
        this.spawnerExplosionListener = new SpawnerExplosionListener(this);
        this.spawnerBreakListener = new SpawnerBreakListener(this);
        this.spawnerPlaceListener = new SpawnerPlaceListener(this);

        // Initialize hopper handler if enabled in config
        setUpHopperHandler();

        // Initialize API implementation
        this.apiImpl = new SmartSpawnerAPIImpl(this);

        // Complete initialization - use Scheduler instead of getServer().getScheduler()
        return asyncInit.thenCompose(unused ->
                Scheduler.supplyAsync(() -> {
                    updateChecker.initialize();
                    return null;
                })
        );
    }

    public void setUpHopperHandler() {
        this.hopperHandler = configManager.getBoolean("hopper-enabled") ? new HopperHandler(this) : null;
    }

    private void registerListeners() {
        PluginManager pm = getServer().getPluginManager();

        // Register core listeners
        pm.registerEvents(globalEventHandlers, this);
        pm.registerEvents(spawnerListGUI, this);
        pm.registerEvents(spawnerBreakListener, this);
        pm.registerEvents(spawnerPlaceListener, this);
        pm.registerEvents(spawnerStorageAction, this);
        pm.registerEvents(spawnerExplosionListener, this);
        pm.registerEvents(spawnerGuiViewManager, this);
        pm.registerEvents(spawnerClickManager, this);
        pm.registerEvents(spawnerMenuAction, this);
        pm.registerEvents(spawnerStackerHandler, this);

        // Register shop integration listeners if available
        if (shopIntegrationManager.isShopGUIPlusEnabled()) {
            pm.registerEvents(new SpawnerHook(this), this);
        }
    }

    private void setupCommand() {
        CommandHandler commandHandler = new CommandHandler(this);
        Objects.requireNonNull(getCommand("smartspawner")).setExecutor(commandHandler);
        Objects.requireNonNull(getCommand("smartspawner")).setTabCompleter(commandHandler);
    }

    private void setupBtatsMetrics() {
        Scheduler.runTask(() -> {
            Metrics metrics = new Metrics(this, 24822);
            metrics.addCustomChart(new Metrics.SimplePie("players",
                    () -> String.valueOf(Bukkit.getOnlinePlayers().size())));
        });
    }

    private void initializeSaleLogging() {
        if (configManager.getBoolean("logging-enabled")) {
            SaleLogger.getInstance();
        }
    }

    private void checkDependencies() {
        // Run protection plugin checks using Scheduler
        Scheduler.runTaskAsync(this::checkProtectionPlugins);

        // Initialize shop integrations
        shopIntegrationManager.initialize();
    }

    private void checkProtectionPlugins() {
        hasWorldGuard = checkPlugin("WorldGuard", () -> {
            try {
                Class.forName("com.sk89q.worldguard.WorldGuard");
                return com.sk89q.worldguard.WorldGuard.getInstance() != null;
            } catch (ClassNotFoundException e) {
                return false;
            }
        }, true);

        hasGriefPrevention = checkPlugin("GriefPrevention", () -> {
            Plugin griefPlugin = Bukkit.getPluginManager().getPlugin("GriefPrevention");
            return griefPlugin != null && griefPlugin instanceof GriefPrevention;
        }, true);

        hasLands = checkPlugin("Lands", () -> {
            Plugin landsPlugin = Bukkit.getPluginManager().getPlugin("Lands");
            if (landsPlugin != null) {
                new Lands(this);
                return true;
            }
            return false;
        }, true);

        hasTowny = checkPlugin("Towny", () -> {
            try {
                Class.forName("com.palmergames.bukkit.towny.TownyAPI");
                return com.palmergames.bukkit.towny.TownyAPI.getInstance() != null;
            } catch (ClassNotFoundException e) {
                return false;
            }
        }, true);

        hasSuperiorSkyblock2 = checkPlugin("SuperiorSkyblock2", () ->
                Bukkit.getPluginManager().getPlugin("SuperiorSkyblock2") != null, true);
    }

    private boolean checkPlugin(String pluginName, PluginCheck checker, boolean logSuccess) {
        try {
            if (checker.check()) {
                if (logSuccess) {
                    getLogger().info(pluginName + " integration enabled successfully!");
                }
                return true;
            }
        } catch (NoClassDefFoundError | NullPointerException e) {
            // Silent fail - plugin not available
        }
        return false;
    }

    @Override
    public void onDisable() {
        saveAndCleanup();
        SpawnerMobHeadTexture.clearCache();
        shutdownSaleLogger();
        getLogger().info("SmartSpawner has been disabled!");
    }

    private void shutdownSaleLogger() {
        if (configManager != null && configManager.getBoolean("logging-enabled")) {
            SaleLogger.getInstance().shutdown();
        }
    }

    private void saveAndCleanup() {
        if (spawnerManager != null) {
            try {
                spawnerManager.saveSpawnerData();
                spawnerManager.cleanupAllSpawners();
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Error saving spawner data during shutdown", e);
            }
        }

        // Clean up resources
        if (rangeChecker != null) rangeChecker.cleanup();
        if (spawnerGuiViewManager != null) spawnerGuiViewManager.cleanup();
        if (hopperHandler != null) hopperHandler.cleanup();
        if (spawnerClickManager != null) spawnerClickManager.cleanup();
        if (spawnerStackerHandler != null) spawnerStackerHandler.cleanupAll();
        if (spawnerStorageUI != null) spawnerStorageUI.cleanup();
        if (updateChecker != null) updateChecker.shutdown();
    }

    @FunctionalInterface
    private interface PluginCheck {
        boolean check();
    }

    public boolean hasShopIntegration() {
        return shopIntegrationManager.hasShopIntegration();
    }

    public IShopIntegration getShopIntegration() {
        return shopIntegrationManager.getShopIntegration();
    }

    // Spawner Provider for ShopGUI+ integration
    public SpawnerProvider getSpawnerProvider() {
        return new SpawnerProvider(this);
    }
}