package me.uc.hussein.ultrasclans.service;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.model.ClanMember;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

/**
 * مستوى العضو وXP والمساهمة (أقسام 11 و66). يرتفع المستوى تلقائيًا: المطلوب للانتقال من
 * المستوى n إلى n+1 هو xp-base × n (قابل للتعديل في config.yml -> member-level).
 * مصادر XP الحالية: القتل، وكل CP يولّده العضو لكلانه (وهو أيضًا "المساهمة").
 */
public final class MemberProgressService {

    private final UltrasClansPlugin plugin;

    public MemberProgressService(UltrasClansPlugin plugin) {
        this.plugin = plugin;
    }

    /** يُستدعى على الـmain thread. CP الممنوح للعضو يُسجَّل كمساهمة ويمنح XP. */
    public void addContribution(UUID uuid, long cp) {
        if (cp <= 0) return;
        plugin.getClanService().getMember(uuid).ifPresent(member -> {
            synchronized (member) {
                long total = (long) member.getContribution() + cp;
                member.setContribution((int) Math.min(Integer.MAX_VALUE, total));
            }
            int xpPerCp = plugin.getConfigManager().get().getInt("member-level.xp-per-cp", 5);
            gain(member, cp * xpPerCp);
        });
    }

    public void addKillXp(UUID uuid) {
        int xp = plugin.getConfigManager().get().getInt("member-level.xp-per-kill", 10);
        plugin.getClanService().getMember(uuid).ifPresent(member -> gain(member, xp));
    }

    private void gain(ClanMember member, long amount) {
        int base = Math.max(1, plugin.getConfigManager().get().getInt("member-level.xp-base", 100));
        int maxLevel = Math.max(1, plugin.getConfigManager().get().getInt("member-level.max-level", 100));
        int newLevel;
        boolean leveled = false;
        synchronized (member) {
            long xp = member.getMemberXp() + amount;
            int level = member.getMemberLevel();
            while (level < maxLevel && xp >= (long) base * level) {
                xp -= (long) base * level;
                level++;
                leveled = true;
            }
            if (level >= maxLevel) xp = 0;
            member.setMemberXp((int) Math.min(Integer.MAX_VALUE, xp));
            member.setMemberLevel(level);
            newLevel = level;
        }
        plugin.getScheduler().runAsync(() -> plugin.getClanService().getMemberRepository().update(member));
        if (leveled) {
            Player online = plugin.getServer().getPlayer(member.getUuid());
            if (online != null) {
                plugin.getMessageService().send(online, "members.level-up", Map.of("level", String.valueOf(newLevel)));
                plugin.playSound(online, "rankup");
            }
        }
    }
}
