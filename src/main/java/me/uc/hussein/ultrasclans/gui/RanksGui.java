package me.uc.hussein.ultrasclans.gui;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.gui.holder.RankEditGuiHolder;
import me.uc.hussein.ultrasclans.gui.holder.RanksGuiHolder;
import me.uc.hussein.ultrasclans.model.Clan;
import me.uc.hussein.ultrasclans.model.ClanPermission;
import me.uc.hussein.ultrasclans.model.ClanRankDefinition;
import me.uc.hussein.ultrasclans.service.RankManagementService;
import me.uc.hussein.ultrasclans.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** قائمة رتب الكلان (حتى 14) + إنشاء رتبة، وواجهة تعديل صلاحيات/أولوية/حذف كل رتبة. */
public final class RanksGui {

    public static final int SIZE = 27;
    public static final int MAX_SHOWN = 14;
    public static final int SLOT_CREATE = 22;
    public static final int SLOT_BACK = 18;

    public static final int EDIT_SIZE = 54;
    public static final int EDIT_BACK = 45;
    public static final int EDIT_PRIORITY_DOWN = 47;
    public static final int EDIT_INFO = 49;
    public static final int EDIT_PRIORITY_UP = 51;
    public static final int EDIT_DELETE = 53;

    private final UltrasClansPlugin plugin;

    public RanksGui(UltrasClansPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, Clan clan) {
        List<ClanRankDefinition> ranks = new ArrayList<>(plugin.getClanService().getRanks(clan.getId()));
        ranks.sort(Comparator.comparingInt(ClanRankDefinition::getPriority).reversed());

        RanksGuiHolder holder = new RanksGuiHolder(player.getUniqueId(), clan.getId());
        holder.setEntries(ranks);
        Inventory inv = Bukkit.createInventory(holder, SIZE, "&8" + clan.getName() + " - Ranks");
        holder.setInventory(inv);

        int slot = 0;
        for (ClanRankDefinition rank : ranks) {
            if (slot >= MAX_SHOWN) break;
            int users = (int) plugin.getClanService().getMembers(clan.getId()).stream()
                    .filter(m -> m.getRankKey().equalsIgnoreCase(rank.getRankKey())).count();
            boolean ownerRank = RankManagementService.isOwnerRank(rank);
            inv.setItem(slot++, new ItemBuilder(ownerRank ? Material.GOLDEN_HELMET : Material.NAME_TAG)
                    .name(rank.getDisplay())
                    .lore("&7Priority: &f" + rank.getPriority(),
                            "&7Permissions: &f" + (ownerRank ? "ALL" : String.valueOf(rank.getPermissions().size())),
                            "&7Members: &f" + users,
                            rank.isProtectedRank() ? "&8Protected" : "",
                            "", "&eClick to open").build());
        }

        int max = plugin.getConfigManager().get().getInt("member-ranks.max-ranks-per-clan", 14);
        inv.setItem(SLOT_CREATE, new ItemBuilder(Material.EMERALD)
                .name("&a&lCreate Rank")
                .lore("&7Ranks: &f" + ranks.size() + "/" + max, "", "&eClick, then type the name in chat").build());
        inv.setItem(SLOT_BACK, new ItemBuilder(Material.ARROW)
                .name(plugin.getMessageService().text(player, "settings.back")).build());
        player.openInventory(inv);
    }

    public void openEdit(Player player, Clan clan, ClanRankDefinition rank) {
        RankEditGuiHolder holder = new RankEditGuiHolder(player.getUniqueId(), clan.getId(), rank.getRankKey());
        Inventory inv = Bukkit.createInventory(holder, EDIT_SIZE, "&8Rank - " + rank.getName());
        holder.setInventory(inv);

        boolean ownerRank = RankManagementService.isOwnerRank(rank);
        boolean canEdit = plugin.getRankManagementService().canManagePermissions(player) && !ownerRank;
        ClanPermission[] all = ClanPermission.values();
        for (int i = 0; i < all.length && i < 45; i++) {
            boolean on = ownerRank || rank.hasPermission(all[i].name());
            inv.setItem(i, new ItemBuilder(on ? Material.LIME_DYE : Material.GRAY_DYE)
                    .name((on ? "&a" : "&7") + all[i].name())
                    .lore(canEdit ? "&eClick to toggle" : "&8Locked").glow(on).build());
        }

        inv.setItem(EDIT_BACK, new ItemBuilder(Material.ARROW)
                .name(plugin.getMessageService().text(player, "settings.back")).build());
        inv.setItem(EDIT_PRIORITY_DOWN, new ItemBuilder(Material.RED_DYE).name("&c-5 Priority").build());
        inv.setItem(EDIT_INFO, new ItemBuilder(Material.NAME_TAG).name(rank.getDisplay())
                .lore("&7Priority: &f" + rank.getPriority()).build());
        inv.setItem(EDIT_PRIORITY_UP, new ItemBuilder(Material.LIME_DYE).name("&a+5 Priority").build());
        inv.setItem(EDIT_DELETE, new ItemBuilder(Material.BARRIER).name("&4&lDelete Rank")
                .lore(rank.isProtectedRank() ? "&8Protected" : "&7Asks for confirmation").build());
        player.openInventory(inv);
    }
}
