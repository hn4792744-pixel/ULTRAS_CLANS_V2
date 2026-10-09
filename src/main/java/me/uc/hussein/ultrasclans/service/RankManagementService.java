package me.uc.hussein.ultrasclans.service;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.model.Clan;
import me.uc.hussein.ultrasclans.model.ClanPermission;
import me.uc.hussein.ultrasclans.model.ClanRankDefinition;
import org.bukkit.entity.Player;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * إنشاء/تعديل/حذف رتب الكلان (حتى 14). كل عملية تتحقق على الخادم من: الصلاحية، حماية رتبة
 * المالك، أن لا يعدّل أحد رتبة أعلى أو مساوية لرتبته، وأن لا يمنح صلاحية لا يملكها هو
 * (منع تصعيد الصلاحيات). المالك مستثنى من قيود الأولوية والمنح فقط.
 */
public final class RankManagementService {

    public enum Fail {
        NO_PERMISSION, PROTECTED, NAME_INVALID, NAME_TAKEN, LIMIT_REACHED, IN_USE,
        ABOVE_YOUR_RANK, CANNOT_GRANT, NOT_FOUND, PRIORITY_RANGE;

        public String messageKey() {
            return "ranks.fail-" + name().toLowerCase(Locale.ROOT).replace('_', '-');
        }
    }

    private static final Pattern NAME = Pattern.compile("^[\\p{L}\\p{N} ]{3,16}$");
    private static final int MAX_EDITABLE_PRIORITY = 89;

    private final UltrasClansPlugin plugin;

    public RankManagementService(UltrasClansPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean canManageRanks(Player p) {
        return plugin.getPermissionService().isOwner(p.getUniqueId())
                || plugin.getPermissionService().hasPermission(p.getUniqueId(), ClanPermission.CLAN_MANAGE_RANKS);
    }

    public boolean canManagePermissions(Player p) {
        return plugin.getPermissionService().isOwner(p.getUniqueId())
                || plugin.getPermissionService().hasPermission(p.getUniqueId(), ClanPermission.CLAN_MANAGE_PERMISSIONS);
    }

    public static boolean isOwnerRank(ClanRankDefinition rank) {
        return rank.getPermissions().contains("*");
    }

    private int actorPriority(Player p, Clan clan) {
        if (plugin.getPermissionService().isOwner(p.getUniqueId())) return 1000;
        return plugin.getClanService().getMember(p.getUniqueId())
                .flatMap(m -> plugin.getClanService().getRank(clan.getId(), m.getRankKey()))
                .map(ClanRankDefinition::getPriority).orElse(0);
    }

    private boolean isOwner(Player p) {
        return plugin.getPermissionService().isOwner(p.getUniqueId());
    }

    /** @return null عند النجاح */
    public Fail create(Player player, Clan clan, String rawName) {
        if (!canManageRanks(player)) return Fail.NO_PERMISSION;
        String name = rawName == null ? "" : rawName.trim();
        if (!NAME.matcher(name).matches()) return Fail.NAME_INVALID;
        String key = name.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", "_");
        if (key.isBlank() || key.equals("_")) return Fail.NAME_INVALID;

        for (ClanRankDefinition r : plugin.getClanService().getRanks(clan.getId())) {
            if (r.getRankKey().equalsIgnoreCase(key) || r.getName().equalsIgnoreCase(name)) return Fail.NAME_TAKEN;
        }
        int max = plugin.getConfigManager().get().getInt("member-ranks.max-ranks-per-clan", 14);
        if (plugin.getClanService().getRanks(clan.getId()).size() >= max) return Fail.LIMIT_REACHED;

        int actor = actorPriority(player, clan);
        int priority = Math.min(20, Math.min(MAX_EDITABLE_PRIORITY, actor - 1));
        if (priority < 1) return Fail.ABOVE_YOUR_RANK;

        Set<String> perms = new LinkedHashSet<>();
        perms.add(ClanPermission.CLAN_VIEW.name());
        plugin.getClanService().addRank(new ClanRankDefinition(clan.getId(), key, name, "&f" + name, "&f",
                priority, false, false, perms));
        audit(player, clan, "RANK_CREATED", key);
        return null;
    }

    public Fail setPermission(Player player, Clan clan, ClanRankDefinition rank, ClanPermission permission, boolean enable) {
        if (!canManagePermissions(player)) return Fail.NO_PERMISSION;
        if (isOwnerRank(rank)) return Fail.PROTECTED;
        if (!isOwner(player) && rank.getPriority() >= actorPriority(player, clan)) return Fail.ABOVE_YOUR_RANK;
        if (enable && !isOwner(player)
                && !plugin.getPermissionService().hasPermission(player.getUniqueId(), permission)) {
            return Fail.CANNOT_GRANT;
        }
        Set<String> updated = new LinkedHashSet<>(rank.getPermissions());
        if (enable) updated.add(permission.name()); else updated.remove(permission.name());
        rank.setPermissions(updated);
        plugin.getClanService().persistRank(rank);
        audit(player, clan, "RANK_PERMISSION_" + (enable ? "GRANT" : "REVOKE"), rank.getRankKey() + ":" + permission.name());
        return null;
    }

    public Fail changePriority(Player player, Clan clan, ClanRankDefinition rank, int delta) {
        if (!canManageRanks(player)) return Fail.NO_PERMISSION;
        if (rank.isProtectedRank() || isOwnerRank(rank)) return Fail.PROTECTED;
        int next = rank.getPriority() + delta;
        if (next < 1 || next > MAX_EDITABLE_PRIORITY) return Fail.PRIORITY_RANGE;
        if (!isOwner(player)) {
            int actor = actorPriority(player, clan);
            if (rank.getPriority() >= actor || next >= actor) return Fail.ABOVE_YOUR_RANK;
        }
        rank.setPriority(next);
        plugin.getClanService().persistRank(rank);
        audit(player, clan, "RANK_PRIORITY", rank.getRankKey() + "=" + next);
        return null;
    }

    public Fail delete(Player player, Clan clan, String rankKey) {
        if (!canManageRanks(player)) return Fail.NO_PERMISSION;
        Optional<ClanRankDefinition> rankOpt = plugin.getClanService().getRank(clan.getId(), rankKey);
        if (rankOpt.isEmpty()) return Fail.NOT_FOUND;
        ClanRankDefinition rank = rankOpt.get();
        if (rank.isProtectedRank() || isOwnerRank(rank)) return Fail.PROTECTED;
        if (!isOwner(player) && rank.getPriority() >= actorPriority(player, clan)) return Fail.ABOVE_YOUR_RANK;
        boolean inUse = plugin.getClanService().getMembers(clan.getId()).stream()
                .anyMatch(m -> m.getRankKey().equalsIgnoreCase(rank.getRankKey()));
        if (inUse) return Fail.IN_USE;
        plugin.getClanService().removeRank(clan.getId(), rank.getRankKey());
        audit(player, clan, "RANK_DELETED", rank.getRankKey());
        return null;
    }

    private void audit(Player player, Clan clan, String action, String detail) {
        String correlation = java.util.UUID.randomUUID().toString();
        plugin.getScheduler().runAsync(() -> plugin.getClanService().getAuditRepository().log(
                correlation, player.getUniqueId(), clan.getId(), action, null, null, detail, "ranks"));
    }
}
