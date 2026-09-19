package de.priyme.singlebiome;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.EnumSet;
import java.util.Set;

public final class MobSuppressListener implements Listener {

    private final WorldManager worldManager;
    private final Set<CreatureSpawnEvent.SpawnReason> blocked;

    public MobSuppressListener(WorldManager worldManager) {
        this.worldManager = worldManager;
        this.blocked = EnumSet.of(
                CreatureSpawnEvent.SpawnReason.NATURAL,
                CreatureSpawnEvent.SpawnReason.CHUNK_GEN,
                CreatureSpawnEvent.SpawnReason.PATROL,
                CreatureSpawnEvent.SpawnReason.REINFORCEMENTS,
                CreatureSpawnEvent.SpawnReason.VILLAGE_INVASION
        );
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!worldManager.isManagedWorld(event.getLocation().getWorld())) return;
        if (blocked.contains(event.getSpawnReason())) {
            event.setCancelled(true);
        }
    }
}
