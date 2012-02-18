package fr.toobian.bukkit.btperms.io;

import fr.toobian.bukkit.btperms.BTPermsPlugin;
import fr.toobian.bukkit.btperms.util.PermissionsEntity;
import fr.toobian.bukkit.btperms.util.PermissionsGroup;
import fr.toobian.bukkit.btperms.util.PermissionsPlayer;
import java.sql.*;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

/**
 *
 * @author Toobian <toobian@toobian.fr>
 */
public class SQLStore implements Store {

    private BTPermsPlugin plugin;
    private Connection connection;
    private String hostname, port, database, username, password;

    public SQLStore(BTPermsPlugin plugin) {
        this.plugin = plugin;
        this.connection = null;
        hostname = plugin.getConfig().getString("sql.hostname");
        port = plugin.getConfig().getString("sql.port");
        database = plugin.getConfig().getString("sql.database");
        username = plugin.getConfig().getString("sql.username");
        password = plugin.getConfig().getString("sql.password");
    }

    private void init() {
        if (connection != null) {
            try {
                connection.close();
                connection = null;
            } catch (SQLException ex) {
                plugin.console.log(Level.SEVERE, null, ex);
            }
        }

        try {
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection("jdbc:mysql://" + hostname + ":" + port + "/" + database, username, password);
        } catch (SQLException ex) {
            plugin.console.log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            plugin.console.log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void addPermission(PermissionsEntity entity, String world, String node, Boolean value) {
        if (entity == null || node == null || value == null) {
            return;
        }
        this.init();
        try {
            String sql;
            String type;
            if (entity instanceof PermissionsGroup) {
                type = "group";
            } else {
                type = "player";
            }

            sql = "INSERT INTO " + type + "s (" + type + "_id, world, permission, value) VALUES (?,?,?,?)";

            PreparedStatement stat = connection.prepareStatement(sql);
            stat.setString(1, entity.getName());
            stat.setString(2, "NULL");
            if (world != null) {
                stat.setString(2, world);
            }
            stat.setString(3, node);
            stat.setBoolean(4, value);
            int executeUpdate = stat.executeUpdate();
            if (executeUpdate == 0) {
                plugin.console.log(Level.SEVERE, "Error with SQLStore : Permission not added");
            }

            stat.close();
        } catch (SQLException ex) {
            plugin.console.log(Level.SEVERE, null, ex);
        } finally {
            try {
                connection.close();
            } catch (SQLException ex) {
                plugin.console.log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public void removePermission(PermissionsEntity entity, String world, String node) {
        if (entity == null || node == null) {
            return;
        }
        this.init();
        try {
            String sql;
            String type;
            if (entity instanceof PermissionsGroup) {
                type = "group";
            } else {
                type = "player";
            }

            sql = "DELETE FROM " + type + "s WHERE " + type + "_id = ? AND world = ? AND permission = ?";

            PreparedStatement stat = connection.prepareStatement(sql);
            stat.setString(1, entity.getName());
            stat.setString(2, "NULL");
            if (world != null) {
                stat.setString(2, world);
            }
            stat.setString(3, node);
            int executeUpdate = stat.executeUpdate();
            if (executeUpdate == 0) {
                plugin.console.log(Level.WARNING, "SQLStore : Permission not deleted or not present");
            }

            stat.close();
        } catch (SQLException ex) {
            plugin.console.log(Level.SEVERE, null, ex);
        } finally {
            try {
                connection.close();
            } catch (SQLException ex) {
                plugin.console.log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public void addParent(PermissionsEntity entity, PermissionsGroup parent) {
        this.init();
        try {
            String sql;
            String type;
            if (entity instanceof PermissionsGroup) {
                type = "group";
            } else {
                if(plugin.getConfig().getBoolean("sql.use_view")) {
                    plugin.console.log(Level.WARNING, "SQL Store: use_view is active, you can not modify group's player");
                    return;
                }
                type = "player";
            }

            sql = "INSERT INTO " + type + "_parents (" + type + "_id, parent) VALUES (?,?)";
            PreparedStatement stat = connection.prepareStatement(sql);
            stat.setString(1, entity.getName());
            stat.setString(2, parent.getName());
            stat.executeUpdate();

            stat.close();
        } catch (SQLException ex) {
            plugin.console.log(Level.SEVERE, null, ex);
        } finally {
            try {
                connection.close();
            } catch (SQLException ex) {
                plugin.console.log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public void removeParent(PermissionsEntity entity, PermissionsGroup parent) {
        this.init();
        try {
            String sql;
            String type;
            if (entity instanceof PermissionsGroup) {
                type = "group";
            } else {
                if(plugin.getConfig().getBoolean("sql.use_view")) {
                    plugin.console.log(Level.WARNING, "SQL Store: use_view is active, you can not modify group's player");
                    return;
                }
                type = "player";
            }

            sql = "DELETE FROM " + type + "_parents WHERE " + type + "_id LIKE ? AND parent LIKE ?";
            PreparedStatement stat = connection.prepareStatement(sql);
            stat.setString(1, entity.getName());
            stat.setString(2, parent.getName());
            stat.executeUpdate();

            stat.close();
        } catch (SQLException ex) {
            plugin.console.log(Level.SEVERE, null, ex);
        } finally {
            try {
                connection.close();
            } catch (SQLException ex) {
                plugin.console.log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public PermissionsGroup getGroupPermissions(String groupName) {
        if (groupName == null) {
            return null;
        }

        PermissionsGroup group = null;

        if (groups.containsKey(groupName)) {
            return groups.get(groupName);
        }

        this.init();
        try {
            String sqlGroup = "SELECT * FROM groups WHERE group_id LIKE ?";
            PreparedStatement stat = connection.prepareStatement(sqlGroup);
            stat.setString(1, groupName);
            ResultSet res = stat.executeQuery();

            if (res.first()) {
                group = new PermissionsGroup(res.getString("group_id"));
                while (!res.isAfterLast()) {
                    String world = res.getString("world");
                    if (world.equals("NULL")) {
                        world = null;
                    }
                    group.addPermission(res.getString("permission"), res.getBoolean("value"), world);
                    res.next();
                }
                group.setWriter(this);
                group.older();
            } else {
                group = new PermissionsGroup(groupName);
                group.setWriter(this);
            }
            
            res.close();
            stat.close();
            connection.close();
            this.init();

            String sqlGroupParents = "SELECT parent FROM group_parents WHERE group_id LIKE ?";
            PreparedStatement statParents = connection.prepareStatement(sqlGroupParents);
            statParents.setString(1, groupName);
            ResultSet resParents = statParents.executeQuery();
            Set<String> parentsName = new HashSet<String>();
            while (resParents.next()) {
                parentsName.add(resParents.getString("parent"));
            }

            Set<PermissionsGroup> parents = new HashSet<PermissionsGroup>();
            for (String parent : parentsName) {
                parents.add(getGroupPermissions(parent));
            }
            
            if(!parents.isEmpty())
                group.setParents(parents);

            groups.put(group.getName(), group);
            resParents.close();
            statParents.close();
        } catch (SQLException ex) {
            plugin.console.log(Level.SEVERE, null, ex);
        } finally {
            try {
                connection.close();
            } catch (SQLException ex) {
                plugin.console.log(Level.SEVERE, null, ex);
            }
        }

        return group;
    }

    @Override
    public PermissionsPlayer getPlayerPermissions(String playerName) {
        if (playerName == null) {
            return null;
        }

        PermissionsPlayer player = null;

        this.init();
        try {
            String sqlPlayer = "SELECT * FROM players WHERE player_id LIKE ?";
            PreparedStatement stat = connection.prepareStatement(sqlPlayer);
            stat.setString(1, playerName);
            ResultSet res = stat.executeQuery();

            if (res.first()) {
                player = new PermissionsPlayer(res.getString("player_id"));
                while (!res.isAfterLast()) {
                    String world = res.getString("world");
                    if (world.equals("NULL")) {
                        world = null;
                    }
                    player.addPermission(res.getString("permission"), res.getBoolean("value"), world);
                    res.next();
                }
                player.setWriter(this);
                player.older();
            } else {
                player = PermissionsPlayer.createNewPlayer(plugin, playerName);
                player.setWriter(this);
            }

            res.close();
            stat.close();
            connection.close();
            this.init();

            String sqlPlayerParents = "SELECT parent FROM player_parents WHERE player_id LIKE ?";
            PreparedStatement statParents = connection.prepareStatement(sqlPlayerParents);
            statParents.setString(1, playerName);
            ResultSet resParents = statParents.executeQuery();
            Set<String> parentsName = new HashSet<String>();
            while (resParents.next()) {
                parentsName.add(resParents.getString("parent"));
            }

            Set<PermissionsGroup> parents = new HashSet<PermissionsGroup>();
            for (String parent : parentsName) {
                parents.add(getGroupPermissions(parent));
            }
            
            if(!parents.isEmpty())
                player.setParents(parents);

            resParents.close();
            statParents.close();
        } catch (SQLException ex) {
            plugin.console.log(Level.SEVERE, null, ex);
        } finally {
            try {
                connection.close();
            } catch (SQLException ex) {
                plugin.console.log(Level.SEVERE, null, ex);
            }
        }

        return player;
    }

    @Override
    public boolean groupExist(String groupName) {
        if (groupName == null) {
            return false;
        }
        this.init();
        boolean exist = false;
        try {
            String sql = "SELECT group_id FROM list_groups WHERE group_id = ?";
            PreparedStatement stat = connection.prepareStatement(sql);
            stat.setString(1, groupName);
            ResultSet res = stat.executeQuery();
            if (res.first()) {
                exist = true;
            }
        } catch (SQLException ex) {
            plugin.console.log(Level.SEVERE, null, ex);
        } finally {
            try {
                connection.close();
            } catch (SQLException ex) {
                plugin.console.log(Level.SEVERE, null, ex);
            }
        }

        return exist;
    }

    @Override
    public boolean playerExist(String playerName) {
        if (playerName == null) {
            return false;
        }
        this.init();
        boolean exist = false;
        try {
            String sql = "SELECT player_id FROM list_players WHERE player_id = ?";
            PreparedStatement stat = connection.prepareStatement(sql);
            stat.setString(1, playerName);
            ResultSet res = stat.executeQuery();
            if (res.first()) {
                exist = true;
            }
        } catch (SQLException ex) {
            plugin.console.log(Level.SEVERE, null, ex);
        } finally {
            try {
                connection.close();
            } catch (SQLException ex) {
                plugin.console.log(Level.SEVERE, null, ex);
            }
        }

        return exist;
    }

    @Override
    public Set<PermissionsPlayer> getPlayersOf(String groupName) {
        if (groupName == null) {
            return null;
        }
        this.init();
        Set<PermissionsPlayer> players = null;
        try {
            String sql = "SELECT player_id FROM player_parents WHERE parent_id = ?";
            PreparedStatement stat = connection.prepareStatement(sql);
            stat.setString(1, groupName);
            ResultSet res = stat.executeQuery();
            if (res.first()) {
                players = new HashSet<PermissionsPlayer>();
                while (!res.isAfterLast()) {
                    String player = res.getString("player_id");
                    players.add(this.getPlayerPermissions(player));
                }
            }
        } catch (SQLException ex) {
            plugin.console.log(Level.SEVERE, null, ex);
        } finally {
            try {
                connection.close();
            } catch (SQLException ex) {
                plugin.console.log(Level.SEVERE, null, ex);
            }
        }

        return players;
    }

    @Override
    public void reload() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Set<PermissionsGroup> getGroups() {
        this.init();
        Set<PermissionsGroup> allGroups = null;
        try {
            String sql = "SELECT group_id FROM list_groups";
            PreparedStatement stat = connection.prepareStatement(sql);
            ResultSet res = stat.executeQuery();
            if (res.first()) {
                allGroups = new HashSet<PermissionsGroup>();
                while (!res.isAfterLast()) {
                    String group = res.getString("group_id");
                    allGroups.add(this.getGroupPermissions(group));
                }
            }
        } catch (SQLException ex) {
            plugin.console.log(Level.SEVERE, null, ex);
        } finally {
            try {
                connection.close();
            } catch (SQLException ex) {
                plugin.console.log(Level.SEVERE, null, ex);
            }
        }

        return allGroups;
    }
}
