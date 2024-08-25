package me.sedattr.deluxeauctions.commands;

import me.sedattr.deluxeauctions.DeluxeAuctions;
import me.sedattr.auctionsapi.AuctionHook;
import me.sedattr.auctionsapi.cache.AuctionCache;
import me.sedattr.deluxeauctions.converters.AuctionMasterConverter;
import me.sedattr.deluxeauctions.converters.ZAuctionHouseConverter;
import me.sedattr.deluxeauctions.inventoryapi.inventory.InventoryAPI;
import me.sedattr.deluxeauctions.managers.Auction;
import me.sedattr.deluxeauctions.managers.Category;
import me.sedattr.deluxeauctions.menus.AuctionsMenu;
import me.sedattr.deluxeauctions.menus.MainMenu;
import me.sedattr.deluxeauctions.others.PlaceholderUtil;
import me.sedattr.deluxeauctions.others.Utils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.time.ZonedDateTime;
import java.util.*;

public class AuctionAdminCommand implements CommandExecutor, TabCompleter {
    private final HashMap<String, List<String>> args = new HashMap<>();

    public AuctionAdminCommand() {
        ConfigurationSection section = DeluxeAuctions.getInstance().configFile.getConfigurationSection("commands");
        if (section == null) {
            this.args.put("reload", Collections.singletonList("reload"));
            this.args.put("menu", Arrays.asList("menu", "open"));
            this.args.put("cancel", Collections.singletonList("cancel"));
            this.args.put("lock", Collections.singletonList("lock"));
            this.args.put("convert", Collections.singletonList("convert"));
        } else {
            this.args.put("reload", section.getStringList("reload"));
            this.args.put("menu", section.getStringList("menu"));
            this.args.put("cancel", section.getStringList("cancel"));
            this.args.put("lock", section.getStringList("lock"));
            this.args.put("convert", section.getStringList("convert"));
        }
    }

    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] args) {
        if (!Utils.hasPermission(commandSender, "admin_commands", "command"))
            return Collections.emptyList();

        ArrayList<String> complete = new ArrayList<>();
        this.args.values().forEach(complete::addAll);

        complete.removeIf(type -> !Utils.hasPermission(commandSender, "admin_commands,", type));

        if (args.length == 1)
            return complete;

        return Collections.emptyList();
    }

    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args) {
        if (!Utils.hasPermission(commandSender, "admin_commands", "command")) {
            Utils.sendMessage(commandSender, "no_permission");
            return false;
        }

        PlaceholderUtil placeholderUtil = new PlaceholderUtil()
                .addPlaceholder("%command_name%", label);

        if (args.length > 0) {
            if (!DeluxeAuctions.getInstance().loaded) {
                Utils.sendMessage(commandSender, "loading");
                return false;
            }

            String lowerCaseArg = args[0].toLowerCase();
            if (this.args.get("cancel").contains(lowerCaseArg)) {
                if (!Utils.hasPermission(commandSender, "admin_commands", "cancel")) {
                    Utils.sendMessage(commandSender, "no_permission");
                    return false;
                }

                if (args.length < 2) {
                    Utils.sendMessage(commandSender, "admin_cancel_usage", placeholderUtil);
                    return false;
                }

                try {
                    UUID uuid = UUID.fromString(args[1]);
                    Auction auction = AuctionCache.getAuction(uuid);
                    if (auction == null)
                        return false;

                    auction.setAuctionEndTime(ZonedDateTime.now().toInstant().getEpochSecond() - 1000);
                    Utils.sendMessage(commandSender, "admin_cancelled", new PlaceholderUtil()
                            .addPlaceholder("%player_displayname%", auction.getAuctionOwnerDisplayName()));
                    return true;
                } catch (Exception e) {
                    Utils.sendMessage(commandSender, "wrong_auction", null);
                }

                return false;
            }

            if (this.args.get("convert").contains(lowerCaseArg)) {
                if (commandSender instanceof Player) {
                    Utils.sendMessage(commandSender, "only_console");
                    return false;
                }

                if (!commandSender.isOp()) {
                    Utils.sendMessage(commandSender, "no_permission");
                    return false;
                }

                if (args.length < 2) {
                    Utils.sendMessage(commandSender, "admin_convert_usage", placeholderUtil);
                    return false;
                }

                if (DeluxeAuctions.getInstance().converting) {
                    Utils.sendMessage(commandSender, "converting");
                    return false;
                }

                long start = System.currentTimeMillis();
                String type = args[1].toLowerCase();
                if (type.equals("auctionmaster") && Bukkit.getPluginManager().isPluginEnabled("AuctionMaster")) {
                    if (new AuctionMasterConverter().convertAuctions())
                        Utils.sendMessage(commandSender, "converted", new PlaceholderUtil()
                                .addPlaceholder("%convert_type%", "AuctionMaster")
                                .addPlaceholder("%convert_time%", String.valueOf(System.currentTimeMillis()-start)));
                } else if (type.equalsIgnoreCase("zauctionhouse") && Bukkit.getPluginManager().isPluginEnabled("zAuctionHouseV3")) {
                    if (new ZAuctionHouseConverter().convertAuctions())
                        Utils.sendMessage(commandSender, "converted", new PlaceholderUtil()
                                .addPlaceholder("%convert_type%", "zAuctionHouse")
                                .addPlaceholder("%convert_time%", String.valueOf(System.currentTimeMillis()-start)));
                } else {
                    Utils.sendMessage(commandSender, "admin_convert_usage", placeholderUtil);
                    return false;
                }

                return true;
            }

            if (this.args.get("lock").contains(lowerCaseArg)) {
                if (!Utils.hasPermission(commandSender, "admin_commands", "lock")) {
                    Utils.sendMessage(commandSender, "no_permission");
                    return false;
                }

                DeluxeAuctions.getInstance().locked = !DeluxeAuctions.getInstance().locked;
                for (Player player : Bukkit.getOnlinePlayers())
                    if (!player.isOp() && InventoryAPI.hasInventory(player))
                        player.closeInventory();

                Utils.sendMessage(commandSender, DeluxeAuctions.getInstance().locked ? "locked" : "unlocked");
                return true;
            }

            if (this.args.get("reload").contains(lowerCaseArg)) {
                if (!Utils.hasPermission(commandSender, "admin_commands", "reload")) {
                    Utils.sendMessage(commandSender, "no_permission");
                    return false;
                }

                long start2 = System.currentTimeMillis();
                DeluxeAuctions.getInstance().reload();

                if (DeluxeAuctions.getInstance().multiServerManager != null)
                    DeluxeAuctions.getInstance().multiServerManager.reload();

                Utils.sendMessage(commandSender, "reloaded", new PlaceholderUtil()
                        .addPlaceholder("%reload_time%", String.valueOf(System.currentTimeMillis() - start2)));
                return true;
            }

            if (this.args.get("menu").contains(lowerCaseArg)) {
                if (!Utils.hasPermission(commandSender, "admin_commands", "menu")) {
                    Utils.sendMessage(commandSender, "no_permission");
                    return false;
                }

                if (args.length < 2) {
                    Utils.sendMessage(commandSender, "admin_menu_usage", placeholderUtil);
                    return false;
                }

                Player b = Bukkit.getPlayerExact(args[1]);
                if (b == null) {
                    Utils.sendMessage(commandSender, "wrong_player", placeholderUtil
                            .addPlaceholder("%player_name%", args[1]));
                    return false;
                }

                if (args.length > 2) {
                    Category category = AuctionHook.getCategory(args[2]);
                    if (category != null) {
                        new AuctionsMenu(b).open(category.getName(), 1);
                        return true;
                    }
                }

                new MainMenu(b).open();
                return true;
            }
        }

        Utils.sendMessage(commandSender, "admin_usage", placeholderUtil);
        return false;
    }
}
