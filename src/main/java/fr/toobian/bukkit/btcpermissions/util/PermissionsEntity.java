package fr.toobian.bukkit.btcpermissions.util;

import fr.toobian.bukkit.btcpermissions.io.Store;
import java.io.Serializable;
import java.util.*;

/**
 *
 * @author Toobian <toobian@toobian.fr>
 */
public abstract class PermissionsEntity implements Serializable {

    protected String name;
    protected LinkedHashMap<String, Boolean> globalPermissions;
    protected LinkedHashMap<String, LinkedHashMap<String, Boolean>> worldsPermissions;
    protected transient Set<PermissionsGroup> parents;
    protected transient Store writer;
    private transient boolean novel;

    public PermissionsEntity(String name) {
        this.name = name;
        parents = new HashSet<PermissionsGroup>();
        globalPermissions = new LinkedHashMap<String, Boolean>();
        worldsPermissions = new LinkedHashMap<String, LinkedHashMap<String, Boolean>>();
        novel = true;
        writer = null;
    }

    public String getName() {
        return name;
    }

    public void setWriter(Store writer) {
        this.writer = writer;
    }

    public boolean isNew() {
        return novel;
    }

    public void older() {
        novel = false;
    }

    @SuppressWarnings("element-type-mismatch")
    public final void addParent(PermissionsGroup parent) {
        if (parent == this) {
            return;
        }
        if (parent.getAllParents().contains(this)) {
            return;
        }

        if (parents.isEmpty()) {
            parents.add(parent);
            this.verifParents();
        } else if (oneParentsHasNoParent(parent.getAllParents())) {
            parents.add(parent);
            this.verifParents();
        }

        if (writer != null) {
            writer.addParent(this, parent);
        }
    }

    public void setParents(Set<PermissionsGroup> parents) {
        this.parents = parents;
        this.verifParents();
    }

    public final void removeParent(PermissionsGroup parent) {
        this.parents.remove(parent);
        if (writer != null) {
            writer.removeParent(this, parent);
        }
    }

    public Set<PermissionsGroup> getParents() {
        return new HashSet<PermissionsGroup>(parents);
    }

    public Set<PermissionsGroup> getAllParents() {
        Set<PermissionsGroup> res = new HashSet<PermissionsGroup>();
        res.addAll(parents);
        for (PermissionsEntity parent : parents) {
            res.addAll(parent.getAllParents());
        }

        return res;
    }

    public boolean hasParent(PermissionsGroup group) {
        if (parents.contains(group)) {
            return true;
        }

        for (PermissionsEntity parent : parents) {
            if (parent.hasParent(group)) {
                return true;
            }
        }

        return false;
    }

    public static boolean oneParentsHasNoParent(Set<PermissionsGroup> parents) {
        for (PermissionsGroup parent : parents) {
            if (parent.parents.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public final void addPermission(String permission, boolean value) {
        globalPermissions.put(permission, value);
        if (writer != null) {
            writer.addPermission(this, null, permission, value);
        }
    }

    public final void addPermission(String permission, boolean value, String world) {
        if (world == null) {
            addPermission(permission, value);
        } else {
            if (!worldsPermissions.containsKey(world)) {
                worldsPermissions.put(world, new LinkedHashMap<String, Boolean>());
            }
            LinkedHashMap<String, Boolean> permissions = worldsPermissions.get(world);
            permissions.put(permission, value);
            if (writer != null) {
                writer.addPermission(this, world, permission, value);
            }
        }
    }

    public final void removePermission(String permission) {
        globalPermissions.remove(permission);
        if (writer != null) {
            writer.removePermission(this, null, permission);
        }
    }

    public final void removePermission(String permission, String world) {
        if (world == null) {
            removePermission(permission);
        } else {
            if (!worldsPermissions.containsKey(world)) {
                return;
            }
            worldsPermissions.get(world).remove(permission);
            if (writer != null) {
                writer.removePermission(this, world, permission);
            }
        }
    }

    public Map<String, Boolean> getPermissions(String world) {
        Map<String, Boolean> permissions = new HashMap<String, Boolean>();

        for (PermissionsEntity parent : parents) {
            Map<String, Boolean> parentPermissions = parent.getPermissions(world);
            Iterator<String> it = parentPermissions.keySet().iterator();
            while (it.hasNext()) {
                String node = it.next();
                if (!permissions.containsKey(node)) {
                    permissions.put(node, parentPermissions.get(node));
                } else if (permissions.get(node)) {
                    permissions.put(node, parentPermissions.get(node));
                }
            }
        }

        permissions.putAll(globalPermissions);
        if (worldsPermissions.containsKey(world)) {
            permissions.putAll(worldsPermissions.get(world));
        }
        return permissions;
    }

    public boolean hasAnyPermission() {
        return !(globalPermissions.isEmpty() || worldsPermissions.isEmpty());
    }
    
    private void verifParents() {
        Set<PermissionsGroup> heritedParents = new HashSet<PermissionsGroup>();
        for(PermissionsGroup parent : parents) {
            Set<PermissionsGroup> tmp = parent.getAllParents();
            heritedParents.addAll(tmp);
        }
        
        parents.removeAll(heritedParents);
    }
}
