package me.sedattr.auctionsapi;

import com.google.common.collect.ImmutableMultimap;
import me.sedattr.auctionsapi.cache.CategoryCache;
import me.sedattr.deluxeauctions.DeluxeAuctions;
import me.sedattr.deluxeauctions.managers.Auction;
import me.sedattr.deluxeauctions.managers.AuctionType;
import me.sedattr.deluxeauctions.managers.Category;
import me.sedattr.deluxeauctions.managers.PlayerBid;
import me.sedattr.deluxeauctions.others.Utils;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuctionHook {
    private static final double MAX_PRICE = 1000000000000.0;
    private static final double MAX_BID = 1000000000000.0;
    private static final int MAX_DURATION = 31556926;
    private static final int MAX_AUCTION = 100;

    public static boolean isAuctionTypeDisabled(String type) {
        if (type == null || type.isEmpty())
            return false;
        if (!type.equalsIgnoreCase("bin") && !type.equalsIgnoreCase("normal"))
            return false;

        boolean setting = DeluxeAuctions.getInstance().configFile.getBoolean(type.toLowerCase() + "_auction.enabled", true);
        return !setting;
    }

    public static double calculatePriceFeePercent(double price, String type) {
        ConfigurationSection feeSection = DeluxeAuctions.getInstance().configFile.getConfigurationSection(type + "_auction.price_fees");
        if (feeSection == null)
            return 0;

        double currentPrice = 0;
        double currentFee = 0;
        for (String key : feeSection.getKeys(false)) {
            double number;
            try {
                number = Double.parseDouble(key);
            } catch (Exception e) {
                continue;
            }

            if (price < number)
                continue;
            if (currentPrice > 0 && currentPrice > number)
                continue;

            currentPrice = number;
            currentFee = feeSection.getDouble(key);
        }

        return Math.max(currentFee, 0);
    }

    public static double calculateDurationFee(long time) {
        ConfigurationSection durationSection = DeluxeAuctions.getInstance().configFile.getConfigurationSection("settings.duration_fee");
        if (durationSection == null)
            return 0;

        String formula = durationSection.getString("formula", "%hours% * 50");
        Expression e = new ExpressionBuilder(formula
                .replace("%weeks%", String.valueOf(time/604800))
                .replace("%days%", String.valueOf(time/86400))
                .replace("%seconds%", String.valueOf(time))
                .replace("%minutes%", String.valueOf(time/60))
                .replace("%hours%", String.valueOf(time/3600)))
                .build();
        double formulaPrice = e.evaluate();

        return Math.max(formulaPrice, durationSection.getDouble("minimum_fee", 50));
    }

    public static double getPriceLimit(Player player, String type) {
        double max = type.equals("price_limit") ? MAX_PRICE : MAX_BID;
        if (player.isOp())
            return max;

        ConfigurationSection section = DeluxeAuctions.getInstance().configFile.getConfigurationSection("player_limits." + type);
        if (section == null)
            return max;

        double current = section.getDouble("default");
        List<String> permissions = section.getStringList("permissions");
        if (permissions.isEmpty())
            return current > 0 ? current : max;

        for (String entry : permissions) {
            String[] args = entry.split("[:]", 2);
            if (args.length < 2)
                continue;

            String permission = args[0];
            double amount = Integer.parseInt(args[1]);

            if (!player.hasPermission(permission))
                continue;

            if (amount > current)
                current = amount;
        }

        return current;
    }

    public static Category getCategory(String name) {
        return CategoryCache.getCategories().get(name);
    }

    public static int getLimit(Player player, String type) {
        int max = type.equals("duration_limit") ? MAX_DURATION : MAX_AUCTION;
        if (player.isOp())
            return max;

        ConfigurationSection section = DeluxeAuctions.getInstance().configFile.getConfigurationSection("player_limits." + type);
        if (section == null)
            return max;

        int current = section.getInt("default");
        List<String> permissions = section.getStringList("permissions");
        if (permissions.isEmpty())
            return current > 0 ? current : max;

        for (String entry : permissions) {
            String[] args = entry.split(":", 2);
            if (args.length < 2)
                continue;

            String permission = args[0];
            int amount = Integer.parseInt(args[1]);

            if (!player.hasPermission(permission))
                continue;

            if (amount > current)
                current = amount;
        }

        return current;
    }

    public static String isSellable(Player player, ItemStack item) {
        if (!Utils.hasPermission(player, "item", item.getType().name()))
            return "no_permission_for_item";

        if (CategoryCache.getItemCategory(item).isEmpty())
            return "unsellable_item";

        if (DeluxeAuctions.getInstance().blacklistHandler.isBlacklisted(item))
            return "blacklisted_item";

        return "";
    }

    public static ItemStack getUpdatedAuctionItem(Auction auction) {
        ItemStack itemStack = auction.getAuctionItem();
        if (itemStack == null)
            return null;
        itemStack = itemStack.clone();

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null)
            return null;

        PlayerBid highestBid = auction.getAuctionBids().getHighestBid();
        String type;
        if (auction.isEnded()) {
            if (highestBid == null)
                type = "expired";
            else
                type = "sold";
        } else {
            if (auction.getAuctionType().equals(AuctionType.BIN))
                type = "not_sold";
            else {
                if (highestBid == null)
                    type = "no_bids";
                else
                    type = "bids";
            }
        }

        ConfigurationSection itemSection = DeluxeAuctions.getInstance().messagesFile.getConfigurationSection("lores.auction_items." + auction.getAuctionType().name().toLowerCase() + "." + type);
        if (itemSection == null)
            return itemStack;

        String displayName = itemSection.getString("name");
        if (displayName != null && meta.hasDisplayName())
            meta.setDisplayName(Utils.colorize(displayName
                    .replace("%item_name%", meta.getDisplayName())));

        List<String> lore = itemSection.getStringList("lore");
        List<String> newLore = new ArrayList<>();
        if (!lore.isEmpty())
            for (String line : lore) {
                if (line.contains("%item_lore%")) {
                    List<String> itemLore = meta.getLore();
                    if (itemLore != null && !itemLore.isEmpty())
                        for (String itemLine : itemLore)
                            newLore.add(Utils.colorize(itemLine));

                    continue;
                }

                newLore.add(Utils.colorize(line
                        .replace("%bid_amount%", String.valueOf(auction.getAuctionBids().getPlayerBids().size()))
                        .replace("%bid_price%", highestBid != null ? DeluxeAuctions.getInstance().numberFormat.format(highestBid.getBidPrice()) : "")
                        .replace("%bidder_displayname%", highestBid != null ? highestBid.getBidOwnerDisplayName() : "")
                        .replace("%buyer_displayname%", highestBid != null ? highestBid.getBidOwnerDisplayName() : "")
                        .replace("%seller_displayname%", auction.getAuctionOwnerDisplayName())
                        .replace("%auction_type%", auction.getAuctionType().name())
                        .replace("%auction_price%", DeluxeAuctions.getInstance().numberFormat.format(auction.getAuctionPrice()))
                        .replace("%auction_time%", DeluxeAuctions.getInstance().timeFormat.formatTime(auction.getAuctionEndTime() - ZonedDateTime.now().toInstant().getEpochSecond(), "auction_times"))
                ));
            }

        meta.setLore(newLore);

        List<String> flags = DeluxeAuctions.getInstance().configFile.getStringList("settings.auction_flags");
        if (!flags.isEmpty()) {
            if (flags.contains("ALL")) {
                if (DeluxeAuctions.getInstance().version > 13)
                    meta.setAttributeModifiers(ImmutableMultimap.of());

                meta.addItemFlags(ItemFlag.values());
            }
            else
                for (String flag : flags)
                    meta.addItemFlags(ItemFlag.valueOf(flag));
        }

        itemStack.setItemMeta(meta);
        return itemStack;
    }
}