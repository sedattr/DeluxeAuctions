package me.sedattr.deluxeauctions.menus;

import me.sedattr.deluxeauctions.DeluxeAuctions;
import me.sedattr.deluxeauctions.api.AuctionHook;
import me.sedattr.deluxeauctions.cache.PlayerCache;
import me.sedattr.deluxeauctions.inventoryapi.HInventory;
import me.sedattr.deluxeauctions.inventoryapi.item.ClickableItem;
import me.sedattr.deluxeauctions.managers.AuctionType;
import me.sedattr.deluxeauctions.managers.PlayerPreferences;
import me.sedattr.deluxeauctions.others.PlaceholderUtil;
import me.sedattr.deluxeauctions.others.Utils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class CreateMenu implements MenuManager {
    private final Player player;
    private ConfigurationSection section;
    private HInventory gui;
    private final PlayerPreferences playerAuction;
    private ItemStack createItem;
    private String type;

    public CreateMenu(Player player) {
        this.player = player;
        this.playerAuction = PlayerCache.getPreferences(player.getUniqueId());

        this.createItem = PlayerCache.getItem(player.getUniqueId());
        this.section = DeluxeAuctions.getInstance().menusFile.getConfigurationSection(this.playerAuction.getCreateType().name().toLowerCase() + "_auction_create_menu");
    }

    public void open(String back) {
        this.type=back;
        this.gui = DeluxeAuctions.getInstance().menuHandler.createInventory(this.player, this.section, "create", null);

        if (!back.equals("command")) {
            int goBackSlot = this.section.getInt("back");
            ItemStack goBackItem = DeluxeAuctions.getInstance().normalItems.get("go_back");

            if (goBackSlot > 0 && goBackItem != null)
                gui.setItem(goBackSlot - 1, ClickableItem.of(goBackItem, (event) -> {
                    if (back.equals("manage"))
                        new ManageMenu(this.player).open(1);
                    else
                        new MainMenu(this.player).open();
                }));
        }

        loadExampleItem();
        loadPriceItem();
        loadSwitchItem();
        loadConfirmItem();
        loadTimeItem();

        this.gui.open(this.player);
    }

    private void loadTimeItem() {
        ConfigurationSection itemSection = this.section.getConfigurationSection("auction_duration");
        if (itemSection == null)
            return;

        PlaceholderUtil placeholderUtil = new PlaceholderUtil()
                .addPlaceholder("%time_fee%", DeluxeAuctions.getInstance().numberFormat.format(AuctionHook.calculateDurationFee(this.playerAuction.getCreateTime())))
                .addPlaceholder("%auction_time%", DeluxeAuctions.getInstance().timeFormat.formatTime(this.playerAuction.getCreateTime(), "other_times"));

        ItemStack itemStack = Utils.createItemFromSection(itemSection, placeholderUtil);
        gui.setItem(itemSection.getInt("slot")-1, ClickableItem.of(itemStack, (event) -> new DurationMenu(this.player).open()));
    }

    private void loadPriceItem() {
        ConfigurationSection itemSection = this.section.getConfigurationSection("select_price");
        if (itemSection == null)
            return;

        double priceFeePercent = AuctionHook.calculatePriceFeePercent(this.playerAuction.getCreatePrice(), this.playerAuction.getCreateType().name().toLowerCase());
        PlaceholderUtil placeholderUtil = new PlaceholderUtil()
                .addPlaceholder("%price_fee%", DeluxeAuctions.getInstance().numberFormat.format(this.playerAuction.getCreatePrice()/100*priceFeePercent))
                .addPlaceholder("%price_fee_percent%", DeluxeAuctions.getInstance().numberFormat.format(priceFeePercent))
                .addPlaceholder("%auction_price%", DeluxeAuctions.getInstance().numberFormat.format(this.playerAuction.getCreatePrice()));

        ItemStack itemStack = Utils.createItemFromSection(itemSection, placeholderUtil);
        gui.setItem(itemSection.getInt("slot")-1, ClickableItem.of(itemStack, (event) -> DeluxeAuctions.getInstance().inputMenu.open(this.player, this)));
    }

    private void loadSwitchItem() {
        ConfigurationSection itemSection = this.section.getConfigurationSection("switch_type");
        if (itemSection == null)
            return;

        ItemStack itemStack = Utils.createItemFromSection(itemSection, null);
        gui.setItem(itemSection.getInt("slot")-1, ClickableItem.of(itemStack, (event) -> {
            AuctionType auctionType = this.playerAuction.getCreateType().equals(AuctionType.BIN) ? AuctionType.NORMAL : AuctionType.BIN;

            if (AuctionHook.isAuctionTypeDisabled(auctionType.name()))
                Utils.sendMessage(player, "disabled_auction_type", new PlaceholderUtil()
                        .addPlaceholder("%auction_type%", auctionType.name()));
            else {
                this.playerAuction.setCreateType(auctionType);
                this.section = DeluxeAuctions.getInstance().menusFile.getConfigurationSection(this.playerAuction.getCreateType().name().toLowerCase() + "_auction_create_menu");
            }

            open(this.type);
        }));
    }

    private void loadConfirmItem() {
        ConfigurationSection itemSection = this.createItem != null ? this.section.getConfigurationSection("confirm_auction.with_item") : this.section.getConfigurationSection("confirm_auction.without_item");
        if (itemSection == null)
            return;

        double priceFeePercent = AuctionHook.calculatePriceFeePercent(this.playerAuction.getCreatePrice(), this.playerAuction.getCreateType().name().toLowerCase());
        double totalFee = this.playerAuction.getCreatePrice()/100*priceFeePercent+AuctionHook.calculateDurationFee(this.playerAuction.getCreateTime());
        PlaceholderUtil placeholderUtil = new PlaceholderUtil()
                .addPlaceholder("%item_displayname%", Utils.getDisplayName(this.createItem))
                .addPlaceholder("%auction_time%", DeluxeAuctions.getInstance().timeFormat.formatTime(this.playerAuction.getCreateTime(), "other_times"))
                .addPlaceholder("%auction_fee%", DeluxeAuctions.getInstance().numberFormat.format(totalFee))
                .addPlaceholder("%auction_price%", DeluxeAuctions.getInstance().numberFormat.format(this.playerAuction.getCreatePrice()));

        ItemStack itemStack = Utils.createItemFromSection(itemSection, placeholderUtil);
        if (this.createItem == null)
            gui.setItem(itemSection.getInt("slot")-1, ClickableItem.empty(itemStack));
        else
            gui.setItem(itemSection.getInt("slot")-1, ClickableItem.of(itemStack, (event) -> {
                double balance = DeluxeAuctions.getInstance().economyManager.getBalance(this.player);
                if (balance < totalFee) {
                    Utils.sendMessage(this.player, "not_enough_money", placeholderUtil.addPlaceholder("%required_money%", DeluxeAuctions.getInstance().numberFormat.format(totalFee-balance)));
                    return;
                }

                new ConfirmMenu(this.player, "confirm_auction").setPrice(totalFee).open();
            }));
    }

    private void loadExampleItem() {
        ConfigurationSection exampleSection = this.createItem == null ? this.section.getConfigurationSection("example_item.without_item") : this.section.getConfigurationSection("example_item.with_item");
        if (exampleSection == null)
            return;

        ItemStack example;
        if (this.createItem == null) {
            example = Utils.createItemFromSection(exampleSection, null);
            if (example == null)
                return;
        } else {
            example = this.createItem.clone();
            ItemMeta meta = example.getItemMeta();
            if (meta == null)
                return;

            String displayName = exampleSection.getString("name");
            if (displayName != null)
                meta.setDisplayName(Utils.colorize(displayName
                    .replace("%item_name%", Utils.getDisplayName(this.createItem))));

            List<String> lore = exampleSection.getStringList("lore");
            List<String> newLore = new ArrayList<>();
            if (!lore.isEmpty())
                for (String line : lore) {
                    if (line.contains("%item_lore%")) {
                        List<String> itemLore = meta.getLore();
                        if (itemLore != null && !itemLore.isEmpty())
                            newLore.addAll(itemLore);

                        continue;
                    }

                    newLore.add(Utils.colorize(line
                            .replace("%item_name%", Utils.getDisplayName(this.createItem))
                    ));
                }

            meta.setLore(newLore);
            example.setItemMeta(meta);
        }

        int slot = exampleSection.getInt("slot");
        this.gui.setItem(slot-1, ClickableItem.of(example, (event) -> {
            if (this.createItem != null) {
                if (!Utils.hasEmptySlot(player)) {
                    Utils.sendMessage(player, "no_empty_slot");
                    return;
                }

                player.getInventory().addItem(this.createItem);
                playerAuction.updateCreate(null);
                this.createItem = null;

                loadExampleItem();
                loadConfirmItem();
                loadPriceItem();
                loadTimeItem();
            }
        }));
    }

    @Override
    public void inputResult(String input) {
        double number;
        double reversedPrice = DeluxeAuctions.getInstance().numberFormat.reverseFormat(input);
        if (reversedPrice > 0)
            number = reversedPrice;
        else {
            try {
                number = Double.parseDouble(input);
            } catch (Exception e) {
                number = 0;
            }
        }

        if (number <= 0)
            Utils.sendMessage(this.player, "wrong_price");
        else {
            int limit = AuctionHook.getLimit(player, "price_limit");
            if (number > limit)
                Utils.sendMessage(player, "reached_price_limit", new PlaceholderUtil()
                        .addPlaceholder("%price_limit%", DeluxeAuctions.getInstance().numberFormat.format((double) limit)));

            this.playerAuction.setCreatePrice(Math.min(number, limit));
        }

        open(this.type);
    }
}
