package me.uc.hussein.ultrasclans.gui.holder;

import me.uc.hussein.ultrasclans.gui.UltrasGuiHolder;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

public final class RankEditGuiHolder implements UltrasGuiHolder {

    private final UUID ownerUuid;
    private final UUID clanId;
    private final String rankKey;
    private Inventory inventory;

    public RankEditGuiHolder(UUID ownerUuid, UUID clanId, String rankKey) {
        this.ownerUuid = ownerUuid;
        this.clanId = clanId;
        this.rankKey = rankKey;
    }

    public void setInventory(Inventory inventory) { this.inventory = inventory; }
    public UUID getClanId() { return clanId; }
    public String getRankKey() { return rankKey; }

    @Override
    public UUID getOwnerUuid() { return ownerUuid; }

    @Override
    public Inventory getInventory() { return inventory; }
}
