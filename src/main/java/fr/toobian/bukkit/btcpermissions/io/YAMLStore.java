package fr.toobian.bukkit.btcpermissions.io;

import fr.toobian.bukkit.btcpermissions.BTCPermissionsPlugin;
import fr.toobian.bukkit.btcpermissions.util.PermissionsEntity;
import fr.toobian.bukkit.btcpermissions.util.PermissionsGroup;
import fr.toobian.bukkit.btcpermissions.util.PermissionsPlayer;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 *
 * @author Toobian <toobian@toobian.fr>
 */
public class YAMLStore implements Store {

    private BTCPermissionsPlugin plugin;
    private FileConfiguration customConfig = null;
    private File customConfigFile = null;

    public YAMLStore(BTCPermissionsPlugin plugin) {
        this.plugin = plugin;
        this.customConfigFile = new File(plugin.getDataFolder(), "permissions.yml");
        customConfig = YamlConfiguration.loadConfiguration(customConfigFile);
    }

    @Override
    public void reload() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void addPermission(PermissionsEntity entity, String world, String node, Boolean value) {
        try {
            ConfigurationSection permissionsSection;
            if (world == null) {
                permissionsSection = getSection(getEntitySection(entity),"permissions");
            } else {
                permissionsSection = getSection(getEntitySection(entity),"worlds." + world);
            }

            permissionsSection.set(node, value);
            customConfig.save(customConfigFile);
        } catch (IOException ex) {
            plugin.console.log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void removePermission(PermissionsEntity entity, String world, String node) {
        try {
            ConfigurationSection permissionsSection;
            if (world == null) {
                permissionsSection = getSection(getEntitySection(entity),"permissions");
            } else {
                permissionsSection = getSection(getEntitySection(entity),"worlds." + world);
            }

            permissionsSection.set(node, null);
            customConfig.save(customConfigFile);
        } catch (IOException ex) {
            plugin.console.log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void addParent(PermissionsEntity entity, PermissionsGroup parent) {
        String path;
        if (entity instanceof PermissionsGroup) {
            path = "inheritance";
        } else {
            path = "groups";
        }
        
        List<String> parents = getEntitySection(entity).getStringList(path);
        
        if(parents != null) {
            try {
                parents.add(parent.getName());
                getEntitySection(entity).set(path, parents);
                customConfig.save(customConfigFile);
            } catch (IOException ex) {
                plugin.console.log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public void removeParent(PermissionsEntity entity, PermissionsGroup parent) {
        String path;
        if (entity instanceof PermissionsGroup) {
            path = "inheritance";
        } else {
            path = "groups";
        }
        
        List<String> parents = getEntitySection(entity).getStringList(path);
        
        if(parents != null) {
            try {
                parents.remove(parent.getName());
                getEntitySection(entity).set(path, parents);
                customConfig.save(customConfigFile);
            } catch (IOException ex) {
                plugin.console.log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public boolean groupExist(String groupName) {
        return customConfig.isSet("groups." + groupName);
    }

    @Override
    public boolean playerExist(String playerName) {
        return customConfig.isSet("users." + playerName);
    }

    @Override
    public Set<PermissionsPlayer> getPlayersOf(String groupName) {
        Set<String> players = customConfig.getConfigurationSection("users").getKeys(false);
        Set<PermissionsPlayer> playersPermissions = new HashSet<PermissionsPlayer>();
        for (String player : players) {
            List<String> groups = customConfig.getConfigurationSection("users."+player).getStringList("groups");
            if(groups.contains(groupName))
                playersPermissions.add(getPlayerPermissions(player));
        }
        return playersPermissions;
    }

    @Override
    public Set<PermissionsGroup> getGroups() {
        Set<String> groupNames = customConfig.getConfigurationSection("groups").getKeys(false);
        Set<PermissionsGroup> groupsPermissions = new HashSet<PermissionsGroup>();
        for (String group : groupNames) {
            groupsPermissions.add(getGroupPermissions(group));
        }
        return groupsPermissions;
    }

    @Override
    public PermissionsGroup getGroupPermissions(String groupName) {
        if (groupName == null) {
            return null;
        }

        if (groups.containsKey(groupName)) {
            return groups.get(groupName);
        }

        PermissionsGroup group;
        if (groupExist(groupName)) {
            group = new PermissionsGroup(groupName);
            ConfigurationSection groupSection = customConfig.getConfigurationSection("groups." + groupName);

            //Parents
            List<String> parents = groupSection.getStringList("inheritance");
            for (String parent : parents) {
                group.addParent(getGroupPermissions(parent));
            }

            //Generals
            Map<String, Object> permissions = getSection(groupSection, "permissions").getValues(true);
            for(String permission : permissions.keySet()) {
                if(permissions.get(permission) instanceof Boolean)
                    group.addPermission(permission, (Boolean) permissions.get(permission));
            }

            //Worlds
            ConfigurationSection worldsSection = getSection(groupSection, "worlds");
            Set<String> worlds = worldsSection.getKeys(false);
            for (String world : worlds) {
                ConfigurationSection worldSection = worldsSection.getConfigurationSection(world);
                permissions = worldSection.getValues(true);
                for(String permission : permissions.keySet()) {
                    if(permissions.get(permission) instanceof Boolean)
                        group.addPermission(permission, (Boolean) permissions.get(permission), world);
                }
            }
            
            group.older();
        } else {
            group = new PermissionsGroup(groupName);
        }
        
        group.setWriter(this);
        groups.put(groupName, group);

        return group;
    }

    @Override
    public PermissionsPlayer getPlayerPermissions(String playerName) {
        
        if (playerName == null) {
            return null;
        }

        PermissionsPlayer player;
        if (playerExist(playerName)) {
            player = new PermissionsPlayer(playerName);
            ConfigurationSection playerSection = customConfig.getConfigurationSection("users." + playerName);

            //Parents
            List<String> parents = playerSection.getStringList("groups");
            for (String parent : parents) {
                player.addParent(getGroupPermissions(parent));
            }

            //Generals
            Map<String, Object> permissions = getSection(playerSection, "permissions").getValues(true);
            for(String permission : permissions.keySet()) {
                if(permissions.get(permission) instanceof Boolean)
                    player.addPermission(permission, (Boolean) permissions.get(permission));
            }

            //Worlds
            ConfigurationSection worldsSection = getSection(playerSection, "worlds");
            Set<String> worlds = worldsSection.getKeys(false);
            for (String world : worlds) {
                ConfigurationSection worldSection = worldsSection.getConfigurationSection(world);
                permissions = worldSection.getValues(true);
                for(String permission : permissions.keySet()) {
                    if(permissions.get(permission) instanceof Boolean)
                        player.addPermission(permission, (Boolean) permissions.get(permission), world);
                }
            }
            player.setWriter(this);
            player.older();
        } else {
            player = PermissionsPlayer.createNewPlayer(plugin, playerName);
            player.setWriter(this);
        }

        return player;
    }
    
    //Private

    private ConfigurationSection getEntitySection(PermissionsEntity entity) {
        String type;
        if (entity instanceof PermissionsGroup) {
            type = "groups";
        } else {
            type = "users";
        }

        return getSection(customConfig, type + "." + entity.getName());
    }

    private static ConfigurationSection getSection(ConfigurationSection section, String path) {
        if (section.isSet(path)) {
            return section.getConfigurationSection(path);
        } else {
            return section.createSection(path);
        }
    }
}
