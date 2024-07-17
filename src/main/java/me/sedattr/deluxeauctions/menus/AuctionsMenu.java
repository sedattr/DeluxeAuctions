package me.sedattr.deluxeauctions.menus;

import me.sedattr.deluxeauctions.api.AuctionHook;
import me.sedattr.deluxeauctions.cache.AuctionCache;
import me.sedattr.deluxeauctions.cache.CategoryCache;
import me.sedattr.deluxeauctions.cache.PlayerCache;
import me.sedattr.deluxeauctions.inventoryapi.inventory.InventoryAPI;
import me.sedattr.deluxeauctions.inventoryapi.HInventory;
import me.sedattr.deluxeauctions.inventoryapi.item.ClickableItem;
import me.sedattr.deluxeauctions.DeluxeAuctions;
import me.sedattr.deluxeauctions.managers.*;
import me.sedattr.deluxeauctions.others.PlaceholderUtil;
import me.sedattr.deluxeauctions.others.Utils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class AuctionsMenu implements MenuManager {
    private final Player player;
    private final PlayerPreferences playerAuction;
    private final ConfigurationSection section;

    private HInventory gui;

    private List<Auction> currentAuctions = new ArrayList<>();
    private List<Auction> filteredAuctions = new ArrayList<>();

    private int totalPage;

    private BukkitTask itemUpdater;
    private List<Integer> slots;

    public AuctionsMenu(Player player) {
        this.player = player;
        this.section = DeluxeAuctions.getInstance().menusFile.getConfigurationSection("auctions_menu");
        this.playerAuction = PlayerCache.getPreferences(player.getUniqueId());
    }

    public void open(String categoryName, int page) {
        if (!categoryName.equalsIgnoreCase("search")) {
            Category category = CategoryCache.getCategories().get(categoryName);
            if (category == null)
                return;

            this.playerAuction.setCategory(category);
            this.slots = DeluxeAuctions.getInstance().categoriesFile.getIntegerList(categoryName + ".slots");
        }

        this.playerAuction.setPage(page);

        Bukkit.getScheduler().runTaskAsynchronously(DeluxeAuctions.getInstance(), () -> {
            updateTotalPage();

            this.currentAuctions = AuctionCache.getOnGoingAuctions(this.filteredAuctions, this.playerAuction.getSortType(), this.playerAuction.getPage(), this.slots.size());
            this.gui = DeluxeAuctions.getInstance().menuHandler.createInventory(this.player, this.section, this.playerAuction.getSearch().isEmpty() ? "auctions" : "search", createPlaceholderUtil());
            DeluxeAuctions.getInstance().menuHandler.addNormalItems(this.player, this.gui, this.section, this.playerAuction.getCategory());

            int goBackSlot = this.section.getInt("back");
            ItemStack goBackItem = DeluxeAuctions.getInstance().normalItems.get("go_back");
            if (goBackSlot > 0 && goBackItem != null)
                gui.setItem(goBackSlot-1, ClickableItem.of(goBackItem, (event) -> new MainMenu(this.player).open()));

            loadInventoryItems();

            Bukkit.getScheduler().runTask(DeluxeAuctions.getInstance(), () -> {
                this.gui.open(this.player);
                updateItems();
            });
        });
    }

    private void loadInventoryItems() {
        loadItems();

        loadSearchItem();
        loadCategories();
        loadCategoriesItem();
        loadFilterItem();
        loadSorterItem();
        loadPageItems();
        loadResetItem();
    }

    private void updateTotalPage() {
        this.filteredAuctions = AuctionCache.getFilteredAuctions(this.playerAuction.getAuctionType(), this.playerAuction.getCategory(), this.playerAuction.getSearch());
        int auctionAmount = this.filteredAuctions.size();

        int totalPage = auctionAmount / this.slots.size() + 1;
        if (auctionAmount <= this.slots.size()*(totalPage-1))
            totalPage--;

        this.totalPage = Math.max(totalPage, 1);
    }

    private PlaceholderUtil createPlaceholderUtil() {
        return new PlaceholderUtil()
                .addPlaceholder("%search%", Utils.colorize(String.valueOf(this.playerAuction.getSearch())))
                .addPlaceholder("%current_page%", String.valueOf(this.playerAuction.getPage()))
                .addPlaceholder("%total_page%", String.valueOf(this.totalPage));
    }

    private void loadPageItems() {
        PlaceholderUtil placeholderUtil = createPlaceholderUtil();

        if (totalPage > this.playerAuction.getPage()) {
            ItemStack nextPage = Utils.createItemFromSection(this.section.getConfigurationSection("next_page"), placeholderUtil);
            if (nextPage != null) {
                this.gui.setItem(this.section.getInt("next_page.slot")-1, ClickableItem.of(nextPage, (event -> {
                    ClickType clickType = event.getClick();
                    if (clickType.equals(ClickType.RIGHT) || clickType.equals(ClickType.SHIFT_RIGHT))
                        open(this.playerAuction.getCategory().getName(), this.totalPage);
                    else
                        open(this.playerAuction.getCategory().getName(), this.playerAuction.getPage()+1);
                })));
            }
        }

        if (this.playerAuction.getPage() > 1) {
            ItemStack previousPage = Utils.createItemFromSection(this.section.getConfigurationSection("previous_page"), placeholderUtil);
            if (previousPage != null)
                this.gui.setItem(this.section.getInt("previous_page.slot")-1, ClickableItem.of(previousPage, (event -> {
                    ClickType clickType = event.getClick();
                    if (clickType.equals(ClickType.RIGHT) || clickType.equals(ClickType.SHIFT_RIGHT))
                        open(this.playerAuction.getCategory().getName(), 1);
                    else
                        open(this.playerAuction.getCategory().getName(), this.playerAuction.getPage()-1);
                })));
        }
    }

    private void loadResetItem() {
        ConfigurationSection resetSection = this.section.getConfigurationSection("reset_settings");
        if (resetSection == null)
            return;
        int slot = resetSection.getInt("slot");

        if (hasResetSettings()) {
            ItemStack reset = Utils.createItemFromSection(resetSection, null);
            gui.setItem(slot-1, ClickableItem.of(reset, (event) -> resetSettings()));
        } else {
            handleEmptyResetSlot(slot);
        }
    }

    private void resetSettings() {
        this.playerAuction.setSearch("");
        this.playerAuction.setAuctionType(DeluxeAuctions.getInstance().auctionType);
        this.playerAuction.setSortType(DeluxeAuctions.getInstance().sortType);

        open(this.playerAuction.getCategory().getName(), 1);
    }

    private void handleEmptyResetSlot(int slot) {
        if (this.section.getIntegerList("glass").contains(slot))
            gui.setItem(slot-1, ClickableItem.empty(this.playerAuction.getCategory().getGlass()));
        else
            gui.setItem(slot-1, null);
    }

    private boolean hasResetSettings() {
        return !this.playerAuction.getSearch().isEmpty() ||
                !this.playerAuction.getAuctionType().equals(DeluxeAuctions.getInstance().auctionType) ||
                !this.playerAuction.getSortType().equals(DeluxeAuctions.getInstance().sortType);
    }

    private void loadSearchItem() {
        ConfigurationSection searchSection = this.section.getConfigurationSection("search");
        if (searchSection != null && searchSection.getBoolean("enabled")) {
            ItemStack search = Utils.createItemFromSection(searchSection, null);
            if (search != null) {
                updateSearchItemLore(search, searchSection);

                int slot = searchSection.getInt("slot");
                gui.setItem(slot - 1, ClickableItem.of(search, event -> handleSearchClick(event.getClick())));
            }
        }
    }

    private void updateSearchItemLore(ItemStack search, ConfigurationSection searchSection) {
        if (!this.playerAuction.getSearch().equalsIgnoreCase("")) {
            Utils.changeLore(search, searchSection.getStringList("lore.selected"), new PlaceholderUtil()
                    .addPlaceholder("%searched%", this.playerAuction.getSearch())
                    .addPlaceholder("%found_items%", String.valueOf(this.filteredAuctions.size())));
        } else
            Utils.changeLore(search, searchSection.getStringList("lore.not_selected"), null);
    }

    private void handleSearchClick(ClickType clickType) {
        if (this.playerAuction.getSearch().equalsIgnoreCase("") || (clickType != ClickType.RIGHT && clickType != ClickType.SHIFT_RIGHT))
            DeluxeAuctions.getInstance().inputMenu.open(this.player, this);
        else {
            this.playerAuction.setSearch("");
            open(this.playerAuction.getCategory().getName(), this.playerAuction.getPage());
        }
    }

    private void loadCategoriesItem() {
        List<String> categories = DeluxeAuctions.getInstance().categoriesFile.getKeys(false).stream().toList();
        if (categories.isEmpty())
            return;

        ConfigurationSection categoriesSection = this.section.getConfigurationSection("categories");
        if (categoriesSection == null)
            return;
        ItemStack categoriesItem = Utils.createItemFromSection(categoriesSection, null);

        List<String> lore = this.section.getStringList("categories.lore");
        if (!lore.isEmpty()) {
            List<String> newLore = new ArrayList<>();

            for (String line : lore) {
                if (line.contains("%category_list%")) {
                    for (String category : categories) {
                        String categoryName = Utils.strip(DeluxeAuctions.getInstance().categoriesFile.getString(category + ".name"));

                        String type;
                        if (category.equals(this.playerAuction.getCategory().getName()))
                            type = "selected";
                        else
                            type = "not_selected";

                        String text = categoriesSection.getString("category." + type);
                        if (text == null)
                            continue;

                        newLore.add(Utils.colorize(text
                                .replace("%category_name%", categoryName)));
                    }
                    continue;
                }

                newLore.add(Utils.colorize(line));
            }

            Utils.changeLore(categoriesItem, newLore, null);
        }
        this.gui.setItem(categoriesSection.getInt("slot")-1, ClickableItem.of(categoriesItem, (event) -> {
            ClickType clickType = event.getClick();

            int number = categories.indexOf(this.playerAuction.getCategory().getName());
            if (clickType.equals(ClickType.RIGHT) || clickType.equals(ClickType.SHIFT_RIGHT)) {
                number--;
                if (number < 0)
                    number = categories.size()-1;
            }
            else {
                number++;
                if (number >= categories.size())
                    number = 0;
            }

            open(categories.get(number), 1);
        }));
    }

    private void loadSorterItem() {
        ItemStack sorterItem = Utils.createItemFromSection(this.section.getConfigurationSection("auction_sorter"), null);
        List<String> lore = this.section.getStringList("auction_sorter.lore." + this.playerAuction.getSortType().name().toLowerCase());
        List<String> newLore = new ArrayList<>();
        if (!lore.isEmpty())
            for (String line : lore)
                newLore.add(Utils.colorize(line));

        Utils.changeLore(sorterItem, newLore, null);

        int slot = this.section.getInt("auction_sorter.slot");
        this.gui.setItem(slot-1, ClickableItem.of(sorterItem, event -> {
            ClickType clickType = event.getClick();
            Utils.playSound(player, "sorter_item_click");

            List<String> types = this.section.getStringList("auction_sorter.types");
            int currentType = !types.isEmpty() && types.contains(this.playerAuction.getSortType().name()) ? types.indexOf(this.playerAuction.getSortType().name()) : 0;

            // backwards
            if (!types.isEmpty()) {
                if (clickType.equals(ClickType.RIGHT) || clickType.equals(ClickType.SHIFT_RIGHT)) {
                    currentType--;
                    if (currentType < 0)
                        currentType = types.size()-1;
                }
                else {
                    currentType++;
                    if (currentType >= types.size())
                        currentType = 0;
                }
            }

            String newType = types.get(currentType);
            this.playerAuction.setSortType(SortType.valueOf(newType.toUpperCase()));

            open(this.playerAuction.getCategory().getName(), Math.min(this.playerAuction.getPage(), this.totalPage));
        }));
    }

    private void loadFilterItem() {
        ItemStack filterItem = Utils.createItemFromSection(this.section.getConfigurationSection("auction_filter"), null);
        List<String> lore = this.section.getStringList("auction_filter.lore." + this.playerAuction.getAuctionType().name().toLowerCase());
        List<String> newLore = new ArrayList<>();
        if (!lore.isEmpty())
            for (String line : lore)
                newLore.add(Utils.colorize(line));

        Utils.changeLore(filterItem, newLore, null);
        int slot = this.section.getInt("auction_filter.slot");
        this.gui.setItem(slot-1, ClickableItem.of(filterItem, event -> {
            ClickType clickType = event.getClick();
            Utils.playSound(player, "filter_item_click");

            List<String> types = this.section.getStringList("auction_filter.types");
            int currentType = !types.isEmpty() && types.contains(this.playerAuction.getAuctionType().name()) ? types.indexOf(this.playerAuction.getAuctionType().name()) : 0;

            // backwards
            if (!types.isEmpty()) {
                if (clickType.equals(ClickType.RIGHT) || clickType.equals(ClickType.SHIFT_RIGHT)) {
                    currentType--;
                    if (currentType < 0)
                        currentType = types.size()-1;
                }
                else {
                    currentType++;
                    if (currentType >= types.size())
                        currentType = 0;
                }
            }

            String newType = types.get(currentType);
            this.playerAuction.setAuctionType(AuctionType.valueOf(newType.toUpperCase()));

            open(this.playerAuction.getCategory().getName(), Math.min(this.playerAuction.getPage(), this.totalPage));
        }));
    }

    private void updateItems() {
        if (this.itemUpdater != null)
            return;

        this.itemUpdater = Bukkit.getScheduler().runTaskTimerAsynchronously(DeluxeAuctions.getInstance(), () -> {
            HInventory inventory = InventoryAPI.getInventory(this.player);
            if (inventory == null || (!inventory.getId().equalsIgnoreCase("auctions") && !inventory.getId().equalsIgnoreCase("search"))) {
                this.itemUpdater.cancel();
                return;
            }

            loadItems();
        }, 0, 20);
    }

    private void loadItems() {
        if (slots.isEmpty())
            return;

        if (!InventoryAPI.hasInventory(this.player))
            return;
        if (this.gui == null)
            return;

        int i = 0;
        for (int slot : slots) {
            if (slot <= 0)
                continue;

            if (i >= this.currentAuctions.size()) {
                this.gui.setItem(slot-1, null);
                continue;
            }

            Auction auction = this.currentAuctions.get(i);
            ItemStack itemStack = AuctionHook.getUpdatedAuctionItem(auction);

            this.gui.setItem(slot-1, ClickableItem.of(itemStack, (event) -> {
                Utils.playSound(this.player, "auction_item_click");

                if (auction.getAuctionType().equals(AuctionType.BIN))
                    new BinViewMenu(this.player, auction).open("auctions");
                else
                    new NormalViewMenu(this.player, auction).open("auctions");
            }));
            i++;
        }
    }

    private void loadPaginationCategories(ConfigurationSection categoriesSection) {
        List<Integer> slots = categoriesSection.getIntegerList("pagination.slots");
        int nextSlot = categoriesSection.getInt("next_page.slot");
        int previousSlot = categoriesSection.getInt("previous_page.slot");

        String fillType =  categoriesSection.getString("pagination.fill_type", "all_glass");
        if (fillType.endsWith("glass")) {
            this.gui.setItem(previousSlot - 1, ClickableItem.empty(this.playerAuction.getCategory().getGlass()));
            this.gui.setItem(nextSlot - 1, ClickableItem.empty(this.playerAuction.getCategory().getGlass()));

            if (fillType.startsWith("page"))
                slots.forEach(a -> this.gui.setItem(a - 1, ClickableItem.empty(null)));
            else
                slots.forEach(a -> this.gui.setItem(a - 1, ClickableItem.empty(this.playerAuction.getCategory().getGlass())));
        } else {
            slots.forEach(a -> this.gui.setItem(a-1, ClickableItem.empty(null)));
            this.gui.setItem(previousSlot-1, ClickableItem.empty(null));
            this.gui.setItem(nextSlot-1, ClickableItem.empty(null));
        }

        List<Category> categories = new ArrayList<>(CategoryCache.getCategories().values());
        categories.sort(Comparator.comparingInt(Category::getPriority));

        int totalPage = CategoryCache.getTotalPage();
        int currentPage = this.playerAuction.getCategoryPage();

        PlaceholderUtil placeholderUtil = new PlaceholderUtil()
                .addPlaceholder("%current_page%", String.valueOf(currentPage))
                .addPlaceholder("%total_page%", String.valueOf(totalPage));

        boolean addPageSlots = categoriesSection.getBoolean("pagination.add_page_slots", true);
        if (currentPage > 1) {
            ItemStack previous = Utils.createItemFromSection(categoriesSection.getConfigurationSection("previous_page"), placeholderUtil);
            this.gui.setItem(previousSlot-1, ClickableItem.of(previous, (event) -> {
                ClickType clickType = event.getClick();
                if (clickType.equals(ClickType.RIGHT) || clickType.equals(ClickType.SHIFT_RIGHT))
                    this.playerAuction.setCategoryPage(1);
                else
                    this.playerAuction.setCategoryPage(currentPage - 1);

                loadPaginationCategories(categoriesSection);
            }));
        } else if (addPageSlots) {
            Category currentCategory = categories.get(0);
            loadCategory(currentCategory, previousSlot-1);

            categories.remove(currentCategory);
        }

        int minimum = slots.size();
        if (addPageSlots)
            minimum++;

        int startIndex = 0;
        int endIndex = Math.min(minimum, categories.size());
        if (currentPage > 1) {
            startIndex = minimum + (currentPage - 2) * slots.size();
            if (currentPage == totalPage)
                endIndex = categories.size();
            else
                endIndex = Math.min(startIndex + slots.size(), categories.size());
        }
        if (addPageSlots)
            slots.add(nextSlot);

        int a = 0;
        for (int i = startIndex; i < endIndex; i++) {
            int slot = slots.get(a);
            Category currentCategory = categories.get(i);
            loadCategory(currentCategory, slot-1);

            a++;
        }

        if (totalPage > currentPage) {
            ItemStack next = Utils.createItemFromSection(categoriesSection.getConfigurationSection("next_page"), placeholderUtil);
            this.gui.setItem(nextSlot-1, ClickableItem.of(next, (event) -> {
                ClickType clickType = event.getClick();
                if (clickType.equals(ClickType.RIGHT) || clickType.equals(ClickType.SHIFT_RIGHT))
                    this.playerAuction.setCategoryPage(totalPage);
                else
                    this.playerAuction.setCategoryPage(currentPage + 1);

                loadPaginationCategories(categoriesSection);
            }));
        }
    }

    private void loadCategories() {
        ConfigurationSection categoriesSection = this.section.getConfigurationSection("category_list");
        if (categoriesSection == null) {
            for (Category auctionCategory : CategoryCache.getCategories().values())
                loadCategory(auctionCategory, auctionCategory.getSlot());
            return;
        }

        String type = categoriesSection.getString("type", "normal");
        if (type.equalsIgnoreCase("pagination")) {
            loadPaginationCategories(categoriesSection);
            return;
        }

        for (Category auctionCategory : CategoryCache.getCategories().values())
            loadCategory(auctionCategory, auctionCategory.getSlot());
    }

    private void loadCategory(Category auctionCategory, int slot) {
        ItemStack categoryItem = auctionCategory.getItem();
        if (categoryItem == null)
            return;
        categoryItem = categoryItem.clone();

        boolean isCategorySame = auctionCategory.getName().equalsIgnoreCase(this.playerAuction.getCategory().getName());
        boolean hasPermission = Utils.hasPermission(this.player, "category", auctionCategory.getName());

        String type;
        if (hasPermission) {
            if (isCategorySame) {
                categoryItem.addUnsafeEnchantment(Enchantment.SILK_TOUCH, 1);
                type = "selected";
            } else
                type = "not_selected";
        }
        else
            type = "no_permission";

        List<String> lore = DeluxeAuctions.getInstance().messagesFile.getStringList("lores.category_items." + type);
        List<String> newLore = new ArrayList<>();
        for (String line : lore) {
            if (line.contains("%category_description%")) {
                List<String> description = auctionCategory.getDescription();
                if (!description.isEmpty()) {
                    for (String desc : description)
                        newLore.add(Utils.colorize(desc
                                .replace("%category_item_amount%", String.valueOf(AuctionCache.getFilteredAuctions(this.playerAuction.getAuctionType(), auctionCategory, this.playerAuction.getSearch()).size()))));
                }

                continue;
            }

            newLore.add(Utils.colorize(line));
        }

        Utils.changeLore(categoryItem, newLore, null);
        this.gui.setItem(slot, isCategorySame || !hasPermission ? ClickableItem.empty(categoryItem) : ClickableItem.of(categoryItem, (event) -> {
            Utils.playSound(this.player, "category_item_click");
            open(auctionCategory.getName(), 1);
        }));
    }

    @Override
    public void inputResult(String input) {
        this.playerAuction.setSearch(input == null ? "" : input);
        open("search", 1);
    }
}