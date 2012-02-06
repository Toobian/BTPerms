package fr.toobian.bukkit.btcpermissions.io;

import fr.toobian.bukkit.btcpermissions.util.PermissionsEntity;
import fr.toobian.bukkit.btcpermissions.util.PermissionsGroup;
import fr.toobian.bukkit.btcpermissions.util.PermissionsPlayer;
import java.util.HashMap;
import java.util.Set;

/**
 *
 * @author Toobian <toobian@toobian.fr>
 */
public interface Store {
    
    public static HashMap<String, PermissionsGroup> groups = new HashMap<String, PermissionsGroup>();
    
    public void reload();
    
    public void addPermission(PermissionsEntity entity, String world, String node, Boolean value);
    public void removePermission(PermissionsEntity entity, String world, String node);
    public void addParent(PermissionsEntity entity, PermissionsGroup parent);
    public void removeParent(PermissionsEntity entity, PermissionsGroup parent);
    
    public boolean groupExist(String groupName);
    public boolean playerExist(String playerName);
    
    public Set<PermissionsPlayer> getPlayersOf(String groupName);
    public Set<PermissionsGroup> getGroups();
    
    public PermissionsGroup getGroupPermissions(String groupName);
    public PermissionsPlayer getPlayerPermissions(String playerName);
}
