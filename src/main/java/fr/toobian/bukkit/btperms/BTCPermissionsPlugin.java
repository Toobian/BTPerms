package fr.toobian.bukkit.btperms;

import fr.toobian.bukkit.btperms.io.SQLStore;
import fr.toobian.bukkit.btperms.io.Store;
import fr.toobian.bukkit.btperms.io.YAMLStore;
import fr.toobian.bukkit.btperms.util.PermissionsPlayer;
import java.util.HashMap;
import java.util.logging.Logger;
import org.bukkit.command.CommandExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 
 * @author Toobian <toobian@toobian.fr>
 */
public class BTCPermissionsPlugin extends JavaPlugin {

    private Listener playerListener;
    private CommandExecutor commandExecutor;
    private FileConfiguration config;
    public Store store;
    public HashMap<String, PermissionAttachment> permissions;
    private HashMap<String, String> lastWorld;
    public Logger console;

    public void onEnable() {
        playerListener = new BTCPermissionsPlayerListener(this);
        commandExecutor = new BTCPermissionsCommand(this);
        permissions = new HashMap<String, PermissionAttachment>();
        lastWorld = new HashMap<String, String>();
        console = this.getServer().getLogger();

        config = this.getConfig();
        this.saveConfig();
        
        String storeName = getConfig().getString("store");
        if(storeName.equals("SQLStore"))
            store = new SQLStore(this);
        else
            store = new YAMLStore(this);
        
        // Commands
        getCommand("permissions").setExecutor(commandExecutor);

        // Events
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(playerListener, this);

        for (Player p : this.getServer().getOnlinePlayers()) {
            registerPlayer(p);
        }

        // Because we are nice
        console.info(this + " is now enabled");
    }

    public void onDisable() {
        // Unregister everyone
        for (Player p : getServer().getOnlinePlayers()) {
            unregisterPlayer(p);
        }

        console.info(this + " is now disabled");
    }

    protected void registerPlayer(Player player) {
        if (permissions.containsKey(player.getName())) {
            unregisterPlayer(player);
        }
        PermissionAttachment attachment = player.addAttachment(this);
        permissions.put(player.getName(), attachment);
        setLastWorld(player.getName(), player.getWorld().getName());
    }

    protected void unregisterPlayer(Player player) {
        if (permissions.containsKey(player.getName())) {
            try {
                player.removeAttachment(permissions.get(player.getName()));
            } catch (IllegalArgumentException ex) {
            }
            permissions.remove(player.getName());
            lastWorld.remove(player.getName());
        } else {
        }
    }

    protected void setLastWorld(String player, String world) {
        if (permissions.containsKey(player) && (lastWorld.get(player) == null || !lastWorld.get(player).equals(world))) {
            lastWorld.put(player, world);
            calculateAttachment(getServer().getPlayer(player), world);
        }
    }

    private void calculateAttachment(Player player, String world) {
        if (player == null) {
            return;
        }
        PermissionAttachment attachment = permissions.get(player.getName());
        if (attachment == null) {
            return;
        }
        
        for (String key : attachment.getPermissions().keySet()) {
            attachment.unsetPermission(key);
        }
        
        PermissionsPlayer playerPermissions = store.getPlayerPermissions(player.getName());
        playerPermissions.load(attachment, world);
        
        player.recalculatePermissions();
    }

    public void refreshPermissions() {
        for (String player : permissions.keySet()) {
            PermissionAttachment attachment = permissions.get(player);
            for (String key : attachment.getPermissions().keySet()) {
                attachment.unsetPermission(key);
            }

            calculateAttachment(getServer().getPlayer(player), getServer().getPlayer(player).getWorld().getName());
        }
    }
}