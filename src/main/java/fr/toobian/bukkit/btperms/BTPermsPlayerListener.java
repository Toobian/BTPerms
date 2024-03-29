package fr.toobian.bukkit.btperms;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;

/**
 *
 * @author Toobian <toobian@toobian.fr>
 */
class BTPermsPlayerListener implements Listener{
    
    private BTPermsPlugin plugin;

    public BTPermsPlayerListener(BTPermsPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority= EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.registerPlayer(event.getPlayer());
    }

    @EventHandler(priority= EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.unregisterPlayer(event.getPlayer());
    }
    
    @EventHandler(priority= EventPriority.MONITOR)
    public void onPlayerKick(PlayerKickEvent event) {
        plugin.unregisterPlayer(event.getPlayer());
    }

    @EventHandler(priority= EventPriority.LOWEST)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        plugin.setLastWorld(event.getFrom().getName(), event.getPlayer().getWorld().getName());
    }
    
    @EventHandler(priority= EventPriority.LOWEST)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        plugin.setLastWorld(event.getPlayer().getName(), event.getTo().getWorld().getName());
    }
}
