package me.sedattr.deluxeauctions.handlers;

import me.sedattr.auctionsapi.cache.EnchantCache;
import me.sedattr.deluxeauctions.DeluxeAuctions;
import me.sedattr.deluxeauctions.configupdater.ConfigUpdater;
import me.sedattr.deluxeauctions.database.MySQLDatabase;
import me.sedattr.deluxeauctions.database.SQLiteDatabase;
import me.sedattr.deluxeauctions.economy.*;
import me.sedattr.deluxeauctions.managers.AuctionType;
import me.sedattr.deluxeauctions.managers.CustomItem;
import me.sedattr.deluxeauctions.managers.SortType;
import me.sedattr.deluxeauctions.menus.InputMenu;
import me.sedattr.deluxeauctions.others.*;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;

public class DataHandler {
    private final File log = new File(DeluxeAuctions.getInstance().getDataFolder(), "logs.txt");
    private final boolean logEnabled = DeluxeAuctions.getInstance().getConfig().getBoolean("settings.enable_log", true);
    private final boolean debugEnabled = DeluxeAuctions.getInstance().getConfig().getBoolean("settings.enable_debug", false);

    public void debug(String message, Logger.LogLevel level) {
        if (this.debugEnabled)
            Logger.sendConsoleMessage(message, level);
    }

    public void writeToLog(String inject) {
        if (!logEnabled)
            return;

        Bukkit.getScheduler().runTaskAsynchronously(DeluxeAuctions.getInstance(), () -> {
            DateFormat simple = new SimpleDateFormat("[MM/dd/yyyy kk:mm:ss] ");
            try {
                FileWriter fileWriter = new FileWriter(this.log, true);

                PrintWriter printWriter = new PrintWriter(fileWriter);
                printWriter.println(simple.format(new Date())+Utils.strip(inject));
                printWriter.close();
            } catch(Exception e) {
                e.printStackTrace();
            }
        });
    }

    public boolean loadDefaultSettings() {
        DeluxeAuctions.getInstance().auctionType = AuctionType.valueOf(DeluxeAuctions.getInstance().configFile.getString("settings.default_filter_type", "ALL").toUpperCase());
        DeluxeAuctions.getInstance().sortType = SortType.valueOf(DeluxeAuctions.getInstance().configFile.getString("settings.default_sort_type", "HIGHEST_PRICE").toUpperCase());
        DeluxeAuctions.getInstance().category = DeluxeAuctions.getInstance().configFile.getString("settings.default_category", "weapons");
        DeluxeAuctions.getInstance().createType = AuctionType.valueOf(DeluxeAuctions.getInstance().configFile.getString("settings.default_type", "BIN").toUpperCase());
        DeluxeAuctions.getInstance().createPrice = DeluxeAuctions.getInstance().configFile.getDouble("settings.default_price", 500);
        DeluxeAuctions.getInstance().createTime = DeluxeAuctions.getInstance().configFile.getInt("settings.default_duration", 21600);

        return DeluxeAuctions.getInstance().category != null;
    }

    public boolean load() {
        DeluxeAuctions.getInstance().configFile = DeluxeAuctions.getInstance().getConfig();
        if (!setupEconomy()) {
            Logger.sendConsoleMessage("There is a problem in economy setup! Plugin is disabling...", Logger.LogLevel.ERROR);
            return false;
        }
        if (!loadDefaultSettings()) {
            Logger.sendConsoleMessage("There is a problem in default category setting (config.yml -> default_category)! Plugin is disabling...", Logger.LogLevel.ERROR);
            return false;
        }

        ConfigurationSection returnSection = DeluxeAuctions.getInstance().configFile.getConfigurationSection("settings.return_category");
        if (returnSection != null && returnSection.getBoolean("enabled"))
            DeluxeAuctions.getInstance().returnCategory = returnSection.getString("category", "miscellaneous");

        checkOldFiles();
        createDefaultFiles();

        setupDatabase();
        DeluxeAuctions.getInstance().menuHandler = new MenuHandler();
        DeluxeAuctions.getInstance().blacklistHandler = new BlacklistHandler();
        DeluxeAuctions.getInstance().numberFormat = new NumberFormat();
        DeluxeAuctions.getInstance().timeFormat = new TimeFormat();
        DeluxeAuctions.getInstance().inputMenu = new InputMenu();

        registerItems();
        loadCustomItems();
        EnchantCache.loadEnchants();

        return true;
    }

    public void loadCustomItems() {
        ConfigurationSection section = DeluxeAuctions.getInstance().itemsFile.getConfigurationSection("custom_items");
        if (section == null)
            return;

        Set<String> keys = section.getKeys(false);
        if (keys.isEmpty())
            return;

        for (String key : keys)
            new CustomItem(key);
    }

    public void checkOldFiles() {
        double currentVersions = DeluxeAuctions.getInstance().getConfig().getDouble("file_versions", 1.0);
        if (DeluxeAuctions.getInstance().fileVersions > currentVersions) {
            long start = System.currentTimeMillis();

            File messagesFile = new File(DeluxeAuctions.getInstance().getDataFolder(), "messages.yml");
            if (messagesFile.exists()) {
                try {
                    File oldBackupFile = new File(DeluxeAuctions.getInstance().getDataFolder() + File.separator + "backups", "messages_backup.yml");
                    if (!oldBackupFile.exists()) {
                        new File(DeluxeAuctions.getInstance().getDataFolder() + File.separator + "backups").mkdirs();

                        Files.copy(Paths.get(messagesFile.getPath()), Paths.get(DeluxeAuctions.getInstance().getDataFolder() + File.separator + "backups" + File.separator + "messages_backup.yml"));
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            File configFile = new File(DeluxeAuctions.getInstance().getDataFolder(), "config.yml");
            if (configFile.exists()) {
                try {
                    File oldBackupFile = new File(DeluxeAuctions.getInstance().getDataFolder() + File.separator + "backups", "config_backup.yml");
                    if (!oldBackupFile.exists()) {
                        new File(DeluxeAuctions.getInstance().getDataFolder() + File.separator + "backups").mkdirs();

                        Files.copy(Paths.get(configFile.getPath()), Paths.get(DeluxeAuctions.getInstance().getDataFolder() + File.separator + "backups" + File.separator + "config_backup.yml"));
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            try {
                ConfigUpdater.update(DeluxeAuctions.getInstance(), "messages.yml", messagesFile);
                ConfigUpdater.update(DeluxeAuctions.getInstance(), "config.yml", configFile);

                /*
                Arrays.asList(
                                "normal_auction.price_fees",
                                "bin_auction.price_fees",
                                "permissions.category.category_list",
                                "permissions.item.item_list",
                                "time_format.convert_times",
                                "player_limits.bid_limit.permissions",
                                "player_limits.duration_limit.permissions",
                                "player_limits.auction_limit.permissions",
                                "player_limits.price_limit.permissions")
                 */
            } catch (IOException e) {
                e.printStackTrace();
            }

            Logger.sendConsoleMessage("It looks like you are using old versions' files! &7(%level_color%Current/New Version: &f" + currentVersions + "/" + DeluxeAuctions.getInstance().fileVersions + "&7)", Logger.LogLevel.WARN);
            Logger.sendConsoleMessage("Config and messages file is successfully updated! (Old files backed up)", Logger.LogLevel.INFO);
            Logger.sendConsoleMessage("Took &f" + (System.currentTimeMillis() - start) + "ms %level_color%to complete.", Logger.LogLevel.INFO);
        }
    }

    public YamlConfiguration getConfiguration(String name) {
        File file = new File(DeluxeAuctions.getInstance().getDataFolder(), name);
        try {
            if (!file.exists())
                DeluxeAuctions.getInstance().saveResource(name, false);

            YamlConfiguration configuration = new YamlConfiguration();
            configuration.load(file);

            return configuration;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void createDefaultFiles() {
        try {
            if (!this.log.exists())
                this.log.createNewFile();
        } catch (Exception e) {
            e.printStackTrace();
        }

        DeluxeAuctions.getInstance().itemsFile = getConfiguration("items.yml");
        DeluxeAuctions.getInstance().categoriesFile = getConfiguration("categories.yml");
        DeluxeAuctions.getInstance().messagesFile = getConfiguration("messages.yml");
        DeluxeAuctions.getInstance().menusFile = getConfiguration("menus.yml");
    }

    public void registerItems() {
        ConfigurationSection itemSection = DeluxeAuctions.getInstance().menusFile.getConfigurationSection("default_items");
        if (itemSection != null) {
            Set<String> keys = itemSection.getKeys(false);
            if (!keys.isEmpty())
                for (String entry : keys) {
                    Object object = itemSection.get(entry);
                    if (object instanceof ItemStack) {
                        DeluxeAuctions.getInstance().normalItems.put(entry, (ItemStack) object);
                        continue;
                    }

                    ItemStack item = Utils.createItemFromSection(itemSection.getConfigurationSection(entry), null);
                    if (item == null)
                        continue;

                    DeluxeAuctions.getInstance().normalItems.put(entry, item);
                }
        }
    }

    public void setupDatabase() {
        if (DeluxeAuctions.getInstance().databaseManager != null)
            return;

        String dataType = DeluxeAuctions.getInstance().getConfig().getString("database.type", "sqlite");
        if (dataType.isEmpty())
            dataType = "sqlite";

        if (dataType.equalsIgnoreCase("mysql"))
            DeluxeAuctions.getInstance().databaseManager = new MySQLDatabase();
        else
            DeluxeAuctions.getInstance().databaseManager = new SQLiteDatabase();
    }

    public Boolean setupEconomy() {
        String economyType = DeluxeAuctions.getInstance().configFile.getString("economy.type", "vault");
        if (economyType.isEmpty())
            return false;

        switch (economyType.toLowerCase()) {
            case "vault":
                if (!Bukkit.getServer().getPluginManager().isPluginEnabled("Vault"))
                    return false;

                RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
                if (rsp == null)
                    return false;

                DeluxeAuctions.getInstance().economy = rsp.getProvider();
                DeluxeAuctions.getInstance().economyManager = new VaultEconomy();
                return true;
            case "edprison":
                if (!Bukkit.getServer().getPluginManager().isPluginEnabled("EdPrison"))
                    return false;

                DeluxeAuctions.getInstance().economyManager = new EdPrisonEconomy();
                return true;
            case "royaleeconomy_balance":
                if (!Bukkit.getServer().getPluginManager().isPluginEnabled("RoyaleEconomy"))
                    return false;

                DeluxeAuctions.getInstance().economyManager = new RoyaleEconomyBalance();
                return true;
            case "royaleeconomy_bank":
                if (!Bukkit.getServer().getPluginManager().isPluginEnabled("RoyaleEconomy"))
                    return false;

                DeluxeAuctions.getInstance().economyManager = new RoyaleEconomyBank();
                return true;
            case "lands":
                if (!Bukkit.getServer().getPluginManager().isPluginEnabled("Lands"))
                    return false;

                DeluxeAuctions.getInstance().economyManager = new LandsEconomy();
                return true;
            case "tokenmanager":
                if (!Bukkit.getServer().getPluginManager().isPluginEnabled("TokenManager"))
                    return false;

                DeluxeAuctions.getInstance().economyManager = new TokenManagerEconomy();
                return true;
            case "playerpoints":
                if (!Bukkit.getServer().getPluginManager().isPluginEnabled("PlayerPoints"))
                    return false;

                DeluxeAuctions.getInstance().economyManager = new PlayerPointsEconomy();
                return true;
            case "ultraeconomy":
                if (!Bukkit.getServer().getPluginManager().isPluginEnabled("UltraEconomy"))
                    return false;

                DeluxeAuctions.getInstance().economyManager = new UltraEconomy();
                return true;
            default:
                DeluxeAuctions.getInstance().economyManager = new YamlEconomy();
                return true;
        }
    }
}