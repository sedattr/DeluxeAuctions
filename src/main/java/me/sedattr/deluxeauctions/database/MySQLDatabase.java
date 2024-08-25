package me.sedattr.deluxeauctions.database;

import me.sedattr.deluxeauctions.DeluxeAuctions;
import me.sedattr.deluxeauctions.addons.multiserver.BungeeAddon;
import me.sedattr.deluxeauctions.addons.multiserver.RedisAddon;
import me.sedattr.auctionsapi.cache.AuctionCache;
import me.sedattr.auctionsapi.cache.PlayerCache;
import me.sedattr.deluxeauctions.managers.*;
import me.sedattr.deluxeauctions.others.Logger;
import me.sedattr.deluxeauctions.others.TaskUtils;
import me.sedattr.deluxeauctions.others.Utils;
import me.sedattr.deluxeauctionsredis.RedisPlugin;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.sql.*;
import java.time.ZonedDateTime;
import java.util.*;

public class MySQLDatabase implements DatabaseManager {
    private Connection connection;
    private String user;
    private String password;
    private String link;
    private String url;

    private String auctions;
    private String items;
    private String stats;

    private void load(ResultSet set) throws SQLException {
        long endTime = set.getLong(7);
        String auctionUUID = set.getString(1);
        UUID uuid = UUID.fromString(auctionUUID);

        long daysTime = DeluxeAuctions.getInstance().configFile.getInt("settings.purge_auctions", 0) * 86400L;
        if (daysTime > 0) {
            daysTime += endTime;
            if (ZonedDateTime.now().toInstant().getEpochSecond() > daysTime) {
                deleteAuction(auctionUUID);
                AuctionCache.removeUpdatingAuction(uuid);
                return;
            }
        }

        UUID owner = UUID.fromString(set.getString(2));
        String displayName = set.getString(3);

        ItemStack item = Utils.itemFromBase64(set.getString(4));
        double price = set.getDouble(6);
        AuctionType type = AuctionType.valueOf(set.getString(8));
        boolean isClaimed = set.getBoolean(9);

        Auction auction = new Auction(uuid, owner, displayName, item, price, type, endTime, isClaimed);
        if (auction.getAuctionCategory().isEmpty()) {
            AuctionCache.removeUpdatingAuction(uuid);
            return;
        }

        /*
        if (check) {
            Auction oldAuction = AuctionCache.getAuction(uuid);
            if (oldAuction == null) {
                    AuctionCreateEvent event = new AuctionCreateEvent(Bukkit.getPlayer(owner), auction);
                    Bukkit.getPluginManager().callEvent(event);
                    if (event.isCancelled())
                        return;
            }
        }
        */

        String bids = set.getString(5);
        if (bids != null) {
            List<PlayerBid> playerBids = new ArrayList<>();

            String[] args = bids.split(",,");
            for (String arg : args) {
                String[] newArgs = arg.split(",");
                if (newArgs.length < 6)
                    continue;

                UUID orderUUID = UUID.fromString(newArgs[0]);
                UUID ownerUUID = UUID.fromString(newArgs[1]);
                String ownerDisplayName = newArgs[2];
                double bidPrice = Double.parseDouble(newArgs[3]);
                long bidTime = Long.parseLong(newArgs[4]);
                boolean collected = Boolean.parseBoolean(newArgs[5]);

                PlayerBid playerBid = new PlayerBid(orderUUID, ownerUUID, ownerDisplayName, bidPrice, bidTime, collected);
                playerBids.add(playerBid);
            }

            auction.getAuctionBids().addPlayerBids(playerBids);
        }

        AuctionCache.addAuction(auction);
        AuctionCache.removeUpdatingAuction(auction.getAuctionUUID());
    }

    public void shutdown() {
        try {
            if (isConnected())
                this.connection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public MySQLDatabase() {
        ConfigurationSection section = DeluxeAuctions.getInstance().getConfig().getConfigurationSection("database.mysql_settings");
        if (section == null) {
            DeluxeAuctions.getInstance().databaseManager = new SQLiteDatabase();
            return;
        }

        this.url = section.getString("url");
        this.link = section.getString("link");
        this.user = section.getString("user");
        this.password = section.getString("password");

        DeluxeAuctions.getInstance().dataHandler.debug("MySQL is connecting...", Logger.LogLevel.INFO);
        this.connection = getConnection();
        if (this.connection == null) {
            DeluxeAuctions.getInstance().dataHandler.debug("MySQL is not connected, changing database type to SQLite!", Logger.LogLevel.WARN);
            DeluxeAuctions.getInstance().databaseManager = new SQLiteDatabase();
            return;
        }
        DeluxeAuctions.getInstance().dataHandler.debug("MySQL is successfully connected!", Logger.LogLevel.INFO);

        String prefix = DeluxeAuctions.getInstance().configFile.getString("database.table_prefix", "");
        this.auctions = prefix + "auctions";
        this.stats = prefix + "stats";
        this.items = prefix + "items";

        try (
                Connection connection = getConnection();
                PreparedStatement statement1 = connection.prepareStatement("CREATE TABLE IF NOT EXISTS " + this.auctions + " (" +
                        "uuid VARCHAR(36) PRIMARY KEY, " +
                        "owner VARCHAR(36), " +
                        "display_name TEXT, " +
                        "item MEDIUMTEXT, " +
                        "bids MEDIUMTEXT, " +
                        "price DOUBLE, " +
                        "end_time INT(11), " +
                        "type TEXT, " +
                        "claimed BOOL);");

                PreparedStatement statement2 = connection.prepareStatement("CREATE TABLE IF NOT EXISTS " + this.items + " (" +
                        "uuid VARCHAR(36) PRIMARY KEY, " +
                        "create_item MEDIUMTEXT);");

                PreparedStatement statement3 = connection.prepareStatement("CREATE TABLE IF NOT EXISTS " + this.stats + " (" +
                        "uuid VARCHAR(36) PRIMARY KEY, " +
                        "won_auctions INTEGER, " +
                        "lost_auctions INTEGER, " +
                        "total_bids INTEGER, " +
                        "highest_bid DOUBLE, " +
                        "spent_money DOUBLE, " +
                        "created_auctions INTEGER, " +
                        "expired_auctions INTEGER, " +
                        "sold_auctions INTEGER, " +
                        "earned_money DOUBLE, " +
                        "total_fees DOUBLE);")
        ) {
            statement1.execute();
            statement2.execute();
            statement3.execute();
        } catch (SQLException x) {
            x.printStackTrace();
        }
    }

    public Connection getConnection() {
        try {
            if (isConnected())
                return this.connection;

            Class.forName(this.link != null && !this.link.isEmpty() ? this.link : "com.mysql.jdbc.Driver");
            return this.connection = DriverManager.getConnection(this.url, this.user, this.password);
        } catch (SQLException | ClassNotFoundException throwable) {
            throwable.printStackTrace();
        }

        return null;
    }

    public boolean isConnected() {
        if (this.connection != null)
            try {
                return !this.connection.isClosed();
            } catch (Exception e) {
                e.printStackTrace();
            }

        return false;
    }

    // DELETE FUNCTIONS
    public void deleteAuction(String uuid) {
        String sql = "DELETE FROM " + this.auctions + " WHERE uuid = ?";
        runTask(() -> {
            try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
                statement.setString(1, uuid);
                statement.execute();
            } catch (SQLException x) {
                handleSQLException(x, () -> deleteAuction(uuid));
            }
        });
    }

    public void deleteItem(UUID uuid) {
        String sql = "DELETE FROM " + this.items + " WHERE uuid = ?";
        runTask(() -> {
            try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
                statement.setString(1, uuid.toString());
                statement.execute();
            } catch (SQLException x) {
                x.printStackTrace();
            }
        });
    }
    //

    public boolean loadAuctions() {
        String sql = "SELECT * FROM " + this.auctions;
        runTask(() -> {
            try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
                ResultSet set = statement.executeQuery();

                long time = System.currentTimeMillis();
                int i = 0;
                while (set.next()) {
                    if (DeluxeAuctions.getInstance().disabled)
                        return;

                    load(set);
                    i++;
                }

                DeluxeAuctions.getInstance().loaded = true;
                if (Bukkit.getPluginManager().isPluginEnabled("DeluxeAuctionsRedis")) {
                    me.sedattr.deluxeauctionsredis.RedisPlugin redis = (RedisPlugin) Bukkit.getPluginManager().getPlugin("DeluxeAuctionsRedis");
                    if (redis != null && redis.isLoaded()) {
                        DeluxeAuctions.getInstance().multiServerManager = new RedisAddon();
                        Logger.sendConsoleMessage("Enabled &fDeluxeAuctions Redis %level_color%support!", Logger.LogLevel.INFO);
                    }
                } else if (DeluxeAuctions.getInstance().configFile.getBoolean("addons.bungeecord", false)) {
                    DeluxeAuctions.getInstance().multiServerManager = new BungeeAddon();
                    Logger.sendConsoleMessage("Enabled &fDeluxeAuctions Bungee %level_color%support!", Logger.LogLevel.INFO);
                }

                Logger.sendConsoleMessage("&f" + i + " %level_color%auctions loaded in &f" + (System.currentTimeMillis()-time) + " ms%level_color%!", Logger.LogLevel.INFO);
            } catch (SQLException x) {
                x.printStackTrace();
            }
        });
        return true;
    }

    public void loadAuction(UUID uuid) {
        String sql = "SELECT * FROM " + this.auctions + " WHERE uuid = ?";
        runTask(() -> {
            try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
                statement.setString(1, uuid.toString());

                ResultSet set = statement.executeQuery();
                if (set.next())
                    load(set);
            } catch (SQLException x) {
                handleSQLException(x, () -> loadAuction(uuid));
            }
        });
    }

    public void loadItem(UUID uuid) {
        String items = "SELECT * FROM " + this.items + " WHERE uuid = ?";

        runTask(() -> {
            try (
                    PreparedStatement itemsStatement = getConnection().prepareStatement(items)
            ) {
                itemsStatement.setString(1, uuid.toString());
                ResultSet item = itemsStatement.executeQuery();

                if (item.next()) {
                    String createItem = item.getString(2);
                    if (createItem != null) {
                        ItemStack newItem = Utils.itemFromBase64(createItem);
                        if (newItem != null)
                            PlayerCache.setItem(uuid, newItem);
                        else
                            PlayerCache.removeItem(uuid);
                    }
                }
            } catch (SQLException x) {
                handleSQLException(x, () -> loadItem(uuid));
            }
        });
    }

    public void loadStat(UUID uuid) {
        String stats = "SELECT * FROM " + this.stats + " WHERE uuid = ?";

        runTask(() -> {
            try (
                    PreparedStatement statsStatement = getConnection().prepareStatement(stats)
            ) {
                statsStatement.setString(1, uuid.toString());
                ResultSet stat = statsStatement.executeQuery();

                if (stat.next()) {
                    int wonAuctions = stat.getInt(2);
                    int lostAuctions = stat.getInt(3);
                    int totalBids = stat.getInt(4);
                    double highestBid = stat.getDouble(5);
                    double spentMoney = stat.getDouble(6);

                    int createdAuctions = stat.getInt(7);
                    int expiredAuctions = stat.getInt(8);
                    int soldAuctions = stat.getInt(9);
                    double earnedMoney = stat.getDouble(10);
                    double totalFees = stat.getDouble(11);

                    PlayerStats data = PlayerCache.getStats(uuid);
                    data.setWonAuctions(wonAuctions);
                    data.setLostAuctions(lostAuctions);
                    data.setTotalBids(totalBids);
                    data.setHighestBid(highestBid);
                    data.setSpentMoney(spentMoney);

                    data.setCreatedAuctions(createdAuctions);
                    data.setExpiredAuctions(expiredAuctions);
                    data.setSoldAuctions(soldAuctions);
                    data.setEarnedMoney(earnedMoney);
                    data.setTotalFees(totalFees);
                }
            } catch (SQLException x) {
                handleSQLException(x, () -> loadStat(uuid));
            }
        });
    }

    // SAVE FUNCTIONS
    public void saveAuctions() {
        String sql = "REPLACE INTO " + this.auctions + " (uuid, owner, display_name, item, bids, price, end_time, type, claimed) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);";
        runTask(() -> {
            try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
                int i = 0;
                long time = System.currentTimeMillis();
                for (Auction auction : AuctionCache.getAuctions().values()) {
                    StringBuilder playerBids = new StringBuilder();
                    List<PlayerBid> bids = auction.getAuctionBids().getPlayerBids();
                    if (!bids.isEmpty()) {
                        for (PlayerBid bid : bids) {
                            String string = bid.toString();

                            playerBids.append(",,").append(string);
                        }

                        playerBids.delete(0, 2);
                    }

                    statement.setString(1, auction.getAuctionUUID().toString());
                    statement.setString(2, auction.getAuctionOwner().toString());
                    statement.setString(3, auction.getAuctionOwnerDisplayName());
                    statement.setString(4, Utils.itemToBase64(auction.getAuctionItem()));
                    statement.setString(5, playerBids.toString());
                    statement.setDouble(6, auction.getAuctionPrice());
                    statement.setLong(7, auction.getAuctionEndTime());
                    statement.setString(8, auction.getAuctionType().name());
                    statement.setBoolean(9, auction.isSellerClaimed());

                    statement.execute();
                    i++;
                }

                Logger.sendConsoleMessage("&f" + i + " %level_color%auctions saved in &f" + (System.currentTimeMillis()-time) + " ms%level_color%!", Logger.LogLevel.INFO);
                DeluxeAuctions.getInstance().converting = false;
            } catch (SQLException x) {
                handleSQLException(x, this::saveAuctions);
            }
        });
    }

    public void saveAuction(Auction auction) {
        String sql = "REPLACE INTO " + this.auctions + " (uuid, owner, display_name, item, bids, price, end_time, type, claimed) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        runTask(() -> {
            StringBuilder playerBids = new StringBuilder();
            List<PlayerBid> bids = auction.getAuctionBids().getPlayerBids();
            if (!bids.isEmpty()) {
                for (PlayerBid bid : bids) {
                    String string = bid.toString();

                    playerBids.append(",,").append(string);
                }

                playerBids.delete(0, 2);
            }

            try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
                statement.setString(1, auction.getAuctionUUID().toString());
                statement.setString(2, auction.getAuctionOwner().toString());
                statement.setString(3, auction.getAuctionOwnerDisplayName());
                statement.setString(4, Utils.itemToBase64(auction.getAuctionItem()));
                statement.setString(5, playerBids.toString());
                statement.setDouble(6, auction.getAuctionPrice());
                statement.setLong(7, auction.getAuctionEndTime());
                statement.setString(8, auction.getAuctionType().name());
                statement.setBoolean(9, auction.isSellerClaimed());

                statement.execute();

                AuctionCache.removeUpdatingAuction(auction.getAuctionUUID());

                if (DeluxeAuctions.getInstance().multiServerManager != null)
                    DeluxeAuctions.getInstance().multiServerManager.loadAuction(auction.getAuctionUUID());
            } catch (SQLException x) {
                handleSQLException(x, () -> {
                    AuctionCache.addUpdatingAuction(auction.getAuctionUUID());
                    saveAuction(auction);
                });
            }
        });
    }

    public void saveItem(UUID uuid, ItemStack item) {
        if (item != null) {
            String base64 = Utils.itemToBase64(item);
            String sql = "REPLACE INTO " + this.items + " (uuid, create_item) VALUES (?, ?)";
            runTask(() -> {
                try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
                    statement.setString(1, uuid.toString());
                    statement.setString(2, base64);

                    statement.execute();
                } catch (SQLException x) {
                    x.printStackTrace();
                }
            });
        } else {
            deleteItem(uuid);
        }
    }

    public void saveStats(PlayerStats stats) {
        UUID uuid = stats.getPlayer();

        String sql = "REPLACE INTO " + this.stats + " (uuid, won_auctions, lost_auctions, total_bids, highest_bid, spent_money, created_auctions, expired_auctions, sold_auctions, earned_money, total_fees) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        runTask(() -> {
            try (
                    PreparedStatement statement = getConnection().prepareStatement(sql)
            ) {
                statement.setString(1, uuid.toString());
                statement.setInt(2, stats.getWonAuctions());
                statement.setInt(3, stats.getLostAuctions());
                statement.setInt(4, stats.getTotalBids());
                statement.setDouble(5, stats.getHighestBid());
                statement.setDouble(6, stats.getSpentMoney());
                statement.setInt(7, stats.getCreatedAuctions());
                statement.setInt(8, stats.getExpiredAuctions());
                statement.setInt(9, stats.getSoldAuctions());
                statement.setDouble(10, stats.getEarnedMoney());
                statement.setDouble(11, stats.getTotalFees());

                statement.execute();

                if (DeluxeAuctions.getInstance().multiServerManager != null)
                    DeluxeAuctions.getInstance().multiServerManager.updateStats(uuid);
            } catch (SQLException x) {
                handleSQLException(x, () -> saveStats(stats));
            }
        });
    }

    private void runTask(Runnable task) {
        if (DeluxeAuctions.getInstance().disabled)
            task.run();
        else
            TaskUtils.runAsync(task);
    }

    private void handleSQLException(SQLException x, Runnable retryTask) {
        if (DeluxeAuctions.getInstance().disabled) {
            retryTask.run();
            return;
        }

        if (x.getMessage().startsWith("[SQLITE_BUSY]"))
            try {
                TaskUtils.runLaterAsync(retryTask, 10);
            } catch (Exception e) {
                TaskUtils.runLater(retryTask, 10);
                e.printStackTrace();
            }
        else
            x.printStackTrace();
    }
}