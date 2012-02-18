package fr.toobian.bukkit.btperms.util;

import fr.toobian.bukkit.btperms.BTPermsPlugin;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;
import org.bukkit.permissions.PermissionAttachment;

/**
 *
 * @author Toobian <toobian@toobian.fr>
 */
public class PermissionsPlayer extends PermissionsEntity implements Serializable{

    public PermissionsPlayer(String player) {
        super(player);
    }
    
    public static PermissionsPlayer createNewPlayer(BTPermsPlugin plugin, String player) {
        PermissionsPlayer pp = new PermissionsPlayer(player);
        
        PermissionsGroup groupPermissions = plugin.store.getGroupPermissions(plugin.getConfig().getString("default_group"));
        pp.addParent(groupPermissions);
        return pp;
    }
    
    public void load(PermissionAttachment permissions, String world) {
        if(world == null)
            return;
        Map<String, Boolean> playerPermissions = getPermissions(world);
        Iterator<String> it = playerPermissions.keySet().iterator();
        while(it.hasNext()) {
            String node = it.next();
            permissions.setPermission(node, playerPermissions.get(node));
        }
    }
}
