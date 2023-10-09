package io.th0rgal.oraxen.hud;

import io.th0rgal.oraxen.OraxenPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class HudTask extends BukkitRunnable {

    private final HudManager manager = OraxenPlugin.get().getHudManager();

    private List<? extends Player> hudEnabledPlayers() {
        return Bukkit.getOnlinePlayers().stream().filter(manager::getHudStateForPlayer).toList();
    }

    @Override
    public void run() {
        for (Player player : hudEnabledPlayers()) {
            Hud hud = manager.getActiveHudForPlayer(player) != null ? manager.getActiveHudForPlayer(player) : manager.getDefaultEnabledHuds().stream().findFirst().orElse(null);

            if (hud == null || manager.getHudID(hud) == null) continue;
            if (hud.disableWhilstInWater() && player.isInWater()) continue;
            if (!player.hasPermission(hud.getPerm())) continue;

            manager.updateHud(player);
        }
    }
}
