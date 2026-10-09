package me.uc.hussein.ultrasclans.gui.holder;

import me.uc.hussein.ultrasclans.gui.UltrasGuiHolder;
import me.uc.hussein.ultrasclans.model.ClanRankDefinition;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.UUID;

public final class RanksGuiHolder implements UltrasGuiHolder {

    private final UUID ownerUuid;
    private final UUID clanId;
    private List<ClanRankDefinition> entries;
    private Inventory inventory;

    public RanksGuiHolder(UUID ownerUuid, UUID clanId) {
        this.ownerUuid = ownerUuid;
        this.clanId = clanId;
    }

    public void setInventory(Inventory inventory) { this.inventory = inventory; }
    public UUID getClanId() { return clanId; }
    public void setEntries(List<ClanRankDefinition> entries) { this.entries = entries; }
    public List<ClanRankDefinition> getEntries() { return entries; }

    @Override
    public UUID getOwnerUuid() { return ownerUuid; }

    @Override
    public Inventory getInventory() { return inventory; }
}
