package fr.toobian.bukkit.btperms;

import fr.toobian.bukkit.btperms.util.PermissionsGroup;
import fr.toobian.bukkit.btperms.util.PermissionsPlayer;
import java.util.*;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachmentInfo;

/**
 *
 * @author Toobian <toobian@toobian.fr>
 */
class BTCPermissionsCommand implements CommandExecutor {

    private BTCPermissionsPlugin plugin;

    public BTCPermissionsCommand(BTCPermissionsPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        String subcommand = args[0];

        if (subcommand.equals("reload")) {
            if (!checkPerm(sender, "reload")) {
                return true;
            }
            plugin.store.reload();
            plugin.refreshPermissions();
            sender.sendMessage(ChatColor.GREEN + "Configuration reloaded.");
            return true;
        } else if (subcommand.equals("group")) {
            if (args.length < 2) {
                if (!checkPerm(sender, "group.help")) {
                    return true;
                }
                return usage(sender, cmd, subcommand);
            }
            groupCommand(sender, cmd, args);
            return true;
        } else if (subcommand.equals("player")) {
            if (args.length < 2) {
                if (!checkPerm(sender, "player.help")) {
                    return true;
                }
                return usage(sender, cmd, subcommand);
            }
            playerCommand(sender, cmd, args);
            return true;
        } else if (subcommand.equals("check")) {
            if (!checkPerm(sender, "check")) {
                return true;
            }
            if (args.length != 2 && args.length != 3) {
                return usage(sender, cmd, subcommand);
            }

            String node = args[1];
            Permissible permissible;
            if (args.length == 2) {
                permissible = sender;
            } else {
                permissible = plugin.getServer().getPlayer(args[2]);
            }

            String name = (permissible instanceof Player) ? ((Player) permissible).getName() : (permissible instanceof ConsoleCommandSender) ? "Console" : "Unknown";

            if (permissible == null) {
                sender.sendMessage(ChatColor.RED + "Player " + ChatColor.WHITE + args[2] + ChatColor.RED + " not found.");
            } else {
                boolean set = permissible.isPermissionSet(node), has = permissible.hasPermission(node);
                String sets = set ? " sets " : " defaults ";
                String perm = has ? " true" : " false";
                sender.sendMessage(ChatColor.GREEN + "Player " + ChatColor.WHITE + name + ChatColor.GREEN + sets + ChatColor.WHITE + node + ChatColor.GREEN + " to " + ChatColor.WHITE + perm + ChatColor.GREEN + ".");
            }
            return true;
        } else if (subcommand.equals("info")) {
            if (!checkPerm(sender, "info")) {
                return true;
            }
            if (args.length != 2) {
                return usage(sender, cmd, subcommand);
            }

            String node = args[1];
            Permission perm = plugin.getServer().getPluginManager().getPermission(node);

            if (perm == null) {
                sender.sendMessage(ChatColor.RED + "Permission " + ChatColor.WHITE + node + ChatColor.RED + " not found.");
            } else {
                sender.sendMessage(ChatColor.GREEN + "Info on permission " + ChatColor.WHITE + perm.getName() + ChatColor.GREEN + ":");
                sender.sendMessage(ChatColor.GREEN + "Default: " + ChatColor.WHITE + perm.getDefault());
                if (perm.getDescription() != null && perm.getDescription().length() > 0) {
                    sender.sendMessage(ChatColor.GREEN + "Description: " + ChatColor.WHITE + perm.getDescription());
                }
                if (perm.getChildren() != null && perm.getChildren().size() > 0) {
                    sender.sendMessage(ChatColor.GREEN + "Children: " + ChatColor.WHITE + perm.getChildren().size());
                }
            }
            return true;
        } else if (subcommand.equals("dump")) {
            if (!checkPerm(sender, "dump")) {
                return true;
            }
            if (args.length < 1 || args.length > 3) {
                return usage(sender, cmd, subcommand);
            }

            int page;
            Permissible permissible;
            if (args.length == 1) {
                permissible = sender;
                page = 1;
            } else if (args.length == 2) {
                try {
                    permissible = sender;
                    page = Integer.parseInt(args[1]);
                } catch (NumberFormatException ex) {
                    permissible = plugin.getServer().getPlayer(args[1]);
                    page = 1;
                }
            } else {
                permissible = plugin.getServer().getPlayer(args[1]);
                try {
                    page = Integer.parseInt(args[2]);
                } catch (NumberFormatException ex) {
                    page = 1;
                }
            }

            if (permissible == null) {
                sender.sendMessage(ChatColor.RED + "Player " + ChatColor.WHITE + args[1] + ChatColor.RED + " not found.");
            } else {
                ArrayList<PermissionAttachmentInfo> dump = new ArrayList<PermissionAttachmentInfo>(permissible.getEffectivePermissions());
                Collections.sort(dump, new Comparator<PermissionAttachmentInfo>() {

                    public int compare(PermissionAttachmentInfo a, PermissionAttachmentInfo b) {
                        return a.getPermission().compareTo(b.getPermission());
                    }
                });

                int numpages = 1 + (dump.size() - 1) / 8;
                if (page > numpages) {
                    page = numpages;
                } else if (page < 1) {
                    page = 1;
                }

                ChatColor g = ChatColor.GREEN, w = ChatColor.WHITE, r = ChatColor.RED;

                int start = 8 * (page - 1);
                sender.sendMessage(ChatColor.RED + "[==== " + ChatColor.GREEN + "Page " + page + " of " + numpages + ChatColor.RED + " ====]");
                for (int i = start; i < start + 8 && i < dump.size(); ++i) {
                    PermissionAttachmentInfo info = dump.get(i);

                    if (info.getAttachment() == null) {
                        sender.sendMessage(g + "Node " + w + info.getPermission() + g + "=" + w + info.getValue() + g + " (" + r + "default" + g + ")");
                    } else {
                        sender.sendMessage(g + "Node " + w + info.getPermission() + g + "=" + w + info.getValue() + g + " (" + w + info.getAttachment().getPlugin().getDescription().getName() + g + ")");
                    }
                }
            }
            return true;
        } else {
            if (!checkPerm(sender, "help")) {
                return true;
            }
            return usage(sender, cmd);
        }
    }

    private boolean groupCommand(CommandSender sender, Command cmd, String[] args) {
        String subcommand = args[1];

        if (subcommand.equals("list")) {
            if (!checkPerm(sender, "group.list")) {
                return true;
            }
            if (args.length != 2) {
                return usage(sender, cmd, "group list");
            }

            String result = "", sep = "";
            for (PermissionsGroup key : plugin.store.getGroups()) {
                result += sep + key.getName();
                sep = ", ";
            }

            sender.sendMessage(ChatColor.GREEN + "Groups: " + ChatColor.WHITE + result);
            return true;
        } else if (subcommand.equals("players")) {
            if (!checkPerm(sender, "group.players")) {
                return true;
            }
            if (args.length != 3) {
                return usage(sender, cmd, "group players");
            }
            String group = args[2];

            if (!plugin.store.groupExist(group)) {
                sender.sendMessage(ChatColor.RED + "No such group " + ChatColor.WHITE + group + ChatColor.RED + ".");
                return true;
            }

            Set<PermissionsPlayer> players = plugin.store.getPlayersOf(group);

            int count = 0;
            String text = "", sep = "";
            for (PermissionsPlayer user : players) {
                ++count;
                text += sep + user.getName();
                sep = ", ";
            }
            sender.sendMessage(ChatColor.GREEN + "Users in " + ChatColor.WHITE + group + ChatColor.GREEN + " (" + ChatColor.WHITE + count + ChatColor.GREEN + "): " + ChatColor.WHITE + text);
            return true;
        } else if (subcommand.equals("setgroup")) {
            if (!checkPerm(sender, "group.setgroup")) {
                return true;
            }
            if (args.length != 4) {
                return usage(sender, cmd, "group setgroup");
            }
            String group = args[2];
            String[] groups = args[3].split(",");

            PermissionsGroup groupPermissions = plugin.store.getGroupPermissions(group);
            Iterator<String> it = Arrays.asList(groups).iterator();
            while (it.hasNext()) {
                PermissionsGroup parentPermissions = plugin.store.getGroupPermissions(it.next());
                groupPermissions.addParent(groupPermissions);
            }

            plugin.refreshPermissions();

            sender.sendMessage(ChatColor.GREEN + "Group " + ChatColor.WHITE + group + ChatColor.GREEN + " is now in " + ChatColor.WHITE + args[3] + ChatColor.GREEN + ".");
            return true;
        } else if (subcommand.equals("addgroup")) {
            if (!checkPerm(sender, "group.addgroup")) {
                return true;
            }
            if (args.length != 4) {
                return usage(sender, cmd, "group addgroup");
            }
            String group = args[2];
            String parentGroup = args[3];

            PermissionsGroup groupPermissions = plugin.store.getGroupPermissions(group);
            groupPermissions.addParent(plugin.store.getGroupPermissions(parentGroup));
            //@todo gérer le non support en fonction du store

            plugin.refreshPermissions();

            sender.sendMessage(ChatColor.GREEN + "Group " + ChatColor.WHITE + group + ChatColor.GREEN + " is now in " + ChatColor.WHITE + parentGroup + ChatColor.GREEN + ".");
            return true;
        } else if (subcommand.equals("removegroup")) {
            if (!checkPerm(sender, "group.removegroup")) {
                return true;
            }
            if (args.length != 4) {
                return usage(sender, cmd, "group removegroup");
            }
            String group = args[2];
            String parentGroup = args[3];

            if (!plugin.store.groupExist(group)) {
                sender.sendMessage(ChatColor.RED + "No such group " + ChatColor.WHITE + group + ChatColor.RED + ".");
                return true;
            }

            PermissionsGroup groupPermissions = plugin.store.getGroupPermissions(group);
            groupPermissions.removeParent(plugin.store.getGroupPermissions(parentGroup));

            plugin.refreshPermissions();

            sender.sendMessage(ChatColor.GREEN + "Group " + ChatColor.WHITE + group + ChatColor.GREEN + " is no longer in " + ChatColor.WHITE + parentGroup + ChatColor.GREEN + ".");
            return true;
        } else if (subcommand.equals("setperm")) {
            if (!checkPerm(sender, "group.setperm")) {
                return true;
            }
            if (args.length != 4 && args.length != 5) {
                return usage(sender, cmd, "group setperm");
            }
            String group = args[2];
            String perm = args[3];
            String world = null;
            boolean value = (args.length == 5) ? Boolean.parseBoolean(args[4]) : true;

            if (!plugin.store.groupExist(group)) {
                sender.sendMessage(ChatColor.RED + "No such group " + ChatColor.WHITE + group + ChatColor.RED + ".");
                return true;
            }

            if (perm.contains(":")) {
                world = perm.substring(0, perm.indexOf(':'));
                perm = perm.substring(perm.indexOf(':') + 1);
            }

            PermissionsGroup groupPermissions = plugin.store.getGroupPermissions(group);
            if (world == null) {
                groupPermissions.addPermission(perm, value);
            } else {
                groupPermissions.addPermission(perm, value, world);
            }

            plugin.refreshPermissions();

            sender.sendMessage(ChatColor.GREEN + "Group " + ChatColor.WHITE + group + ChatColor.GREEN + " now has " + ChatColor.WHITE + perm + ChatColor.GREEN + " = " + ChatColor.WHITE + value + ChatColor.GREEN + ".");
            return true;
        } else if (subcommand.equals("unsetperm")) {
            if (!checkPerm(sender, "group.unsetperm")) {
                return true;
            }
            if (args.length != 4) {
                return usage(sender, cmd, "group unsetperm");
            }
            String group = args[2];
            String perm = args[3];
            String world = null;

            if (!plugin.store.groupExist(group)) {
                sender.sendMessage(ChatColor.RED + "No such group " + ChatColor.WHITE + group + ChatColor.RED + ".");
                return true;
            }

            if (perm.contains(":")) {
                world = perm.substring(0, perm.indexOf(':'));
                perm = perm.substring(perm.indexOf(':') + 1);
            }

            PermissionsGroup groupPermissions = plugin.store.getGroupPermissions(group);
            if (world == null) {
                groupPermissions.removePermission(perm);
            } else {
                groupPermissions.removePermission(perm, world);
            }

            plugin.refreshPermissions();

            sender.sendMessage(ChatColor.GREEN + "Group " + ChatColor.WHITE + group + ChatColor.GREEN + " no longer has " + ChatColor.WHITE + perm + ChatColor.GREEN + " set.");
            return true;
        } else {
            if (!checkPerm(sender, "group.help")) {
                return true;
            }
            return usage(sender, cmd);
        }
    }

    private boolean playerCommand(CommandSender sender, Command cmd, String[] args) {
        String subcommand = args[1];

        if (subcommand.equals("groups")) {
            if (!checkPerm(sender, "player.groups")) {
                return true;
            }
            if (args.length != 3) {
                return usage(sender, cmd, "player groups");
            }
            String player = args[2];

            if (!plugin.store.playerExist(player)) {
                sender.sendMessage(ChatColor.GREEN + "Player " + ChatColor.WHITE + player + ChatColor.RED + " is in the default group.");
                return true;
            }

            PermissionsPlayer playerPermissions = plugin.store.getPlayerPermissions(player);
            Set<PermissionsGroup> parents = playerPermissions.getParents();

            int count = 0;
            String text = "", sep = "";
            for (PermissionsGroup group : parents) {
                ++count;
                text += sep + group.getName();
                sep = ", ";
            }
            sender.sendMessage(ChatColor.GREEN + "Player " + ChatColor.WHITE + player + ChatColor.GREEN + " is in groups (" + ChatColor.WHITE + count + ChatColor.GREEN + "): " + ChatColor.WHITE + text);
            return true;
        } else if (subcommand.equals("setgroup")) {
            if (!checkPerm(sender, "player.setgroup")) {
                return true;
            }
            if (args.length != 4) {
                return usage(sender, cmd, "player setgroup");
            }
            String player = args[2];
            String[] groups = args[3].split(",");

            PermissionsPlayer playerPermissions = plugin.store.getPlayerPermissions(player);
            Iterator<String> it = Arrays.asList(groups).iterator();
            while (it.hasNext()) {
                PermissionsGroup groupPermissions = plugin.store.getGroupPermissions(it.next());
                playerPermissions.addParent(groupPermissions);
            }

            plugin.refreshPermissions();

            sender.sendMessage(ChatColor.GREEN + "Player " + ChatColor.WHITE + player + ChatColor.GREEN + " is now in " + ChatColor.WHITE + args[3] + ChatColor.GREEN + ".");
            return true;
        } else if (subcommand.equals("addgroup")) {
            if (!checkPerm(sender, "player.addgroup")) {
                return true;
            }
            if (args.length != 4) {
                return usage(sender, cmd, "player addgroup");
            }
            String player = args[2];
            String group = args[3];

            PermissionsPlayer playerPermissions = plugin.store.getPlayerPermissions(player);
            playerPermissions.addParent(plugin.store.getGroupPermissions(group));
            //@todo gérer le non support en fonction du store

            plugin.refreshPermissions();

            sender.sendMessage(ChatColor.GREEN + "Player " + ChatColor.WHITE + player + ChatColor.GREEN + " is now in " + ChatColor.WHITE + group + ChatColor.GREEN + ".");
            return true;
        } else if (subcommand.equals("removegroup")) {
            if (!checkPerm(sender, "player.removegroup")) {
                return true;
            }
            if (args.length != 4) {
                return usage(sender, cmd, "player removegroup");
            }
            String player = args[2];
            String group = args[3];

            if (!plugin.store.playerExist(player)) {
                sender.sendMessage(ChatColor.RED + "No such player " + ChatColor.WHITE + player + ChatColor.RED + ".");
                return true;
            }

            PermissionsPlayer playerPermissions = plugin.store.getPlayerPermissions(player);
            playerPermissions.removeParent(plugin.store.getGroupPermissions(group));

            plugin.refreshPermissions();

            sender.sendMessage(ChatColor.GREEN + "Player " + ChatColor.WHITE + player + ChatColor.GREEN + " is no longer in " + ChatColor.WHITE + group + ChatColor.GREEN + ".");
            return true;
        } else if (subcommand.equals("setperm")) {
            if (!checkPerm(sender, "player.setperm")) {
                return true;
            }
            if (args.length != 4 && args.length != 5) {
                return usage(sender, cmd, "player setperm");
            }
            String player = args[2];
            String perm = args[3];
            String world = null;
            boolean value = (args.length == 5) ? Boolean.parseBoolean(args[4]) : true;

            if (perm.contains(":")) {
                world = perm.substring(0, perm.indexOf(':'));
                perm = perm.substring(perm.indexOf(':') + 1);
            }

            PermissionsPlayer playerPermissions = plugin.store.getPlayerPermissions(player);
            if (world == null) {
                playerPermissions.addPermission(perm, value);
            } else {
                playerPermissions.addPermission(perm, value, world);
            }

            plugin.refreshPermissions();

            sender.sendMessage(ChatColor.GREEN + "Player " + ChatColor.WHITE + player + ChatColor.GREEN + " now has " + ChatColor.WHITE + perm + ChatColor.GREEN + " = " + ChatColor.WHITE + value + ChatColor.GREEN + ".");
            return true;
        } else if (subcommand.equals("unsetperm")) {
            if (!checkPerm(sender, "player.unsetperm")) {
                return true;
            }
            if (args.length != 4) {
                return usage(sender, cmd, "player unsetperm");
            }
            String player = args[2];
            String perm = args[3];
            String world = null;

            if (!plugin.store.playerExist(player)) {
                sender.sendMessage(ChatColor.RED + "No such player " + ChatColor.WHITE + player + ChatColor.RED + ".");
                return true;
            }

            if (perm.contains(":")) {
                world = perm.substring(0, perm.indexOf(':'));
                perm = perm.substring(perm.indexOf(':') + 1);
            }

            PermissionsPlayer playerPermissions = plugin.store.getPlayerPermissions(player);
            if (world == null) {
                playerPermissions.removePermission(perm);
            } else {
                playerPermissions.removePermission(perm, world);
            }

            plugin.refreshPermissions();

            sender.sendMessage(ChatColor.GREEN + "Player " + ChatColor.WHITE + player + ChatColor.GREEN + " no longer has " + ChatColor.WHITE + perm + ChatColor.GREEN + " set.");
            return true;
        } else {
            if (!checkPerm(sender, "player.help")) {
                return true;
            }
            return usage(sender, cmd);
        }
    }

    private boolean checkPerm(CommandSender sender, String subnode) {
        boolean ok = sender.hasPermission("permissions." + subnode) || sender.hasPermission("permissions.*");
        if (!ok) {
            sender.sendMessage(ChatColor.RED + "You do not have permissions to do that.");
        }
        return ok;
    }

    private boolean usage(CommandSender sender, Command command) {
        sender.sendMessage(ChatColor.RED + "[====" + ChatColor.GREEN + " /permissons " + ChatColor.RED + "====]");
        for (String line : command.getUsage().split("\\n")) {
            if ((line.startsWith("/<command> group") && !line.startsWith("/<command> group -"))
                    || (line.startsWith("/<command> player") && !line.startsWith("/<command> player -"))) {
                continue;
            }
            sender.sendMessage(formatLine(line));
        }
        return true;
    }

    private boolean usage(CommandSender sender, Command command, String subcommand) {
        sender.sendMessage(ChatColor.RED + "[====" + ChatColor.GREEN + " /permissons " + subcommand + " " + ChatColor.RED + "====]");
        for (String line : command.getUsage().split("\\n")) {
            if (line.startsWith("/<command> " + subcommand)) {
                sender.sendMessage(formatLine(line));
            }
        }
        return true;
    }

    private String formatLine(String line) {
        int i = line.indexOf(" - ");
        String usage = line.substring(0, i);
        String desc = line.substring(i + 3);

        usage = usage.replace("<command>", "permissions");
        usage = usage.replaceAll("\\[[^]:]+\\]", ChatColor.AQUA + "$0" + ChatColor.GREEN);
        usage = usage.replaceAll("\\[[^]]+:\\]", ChatColor.AQUA + "$0" + ChatColor.LIGHT_PURPLE);
        usage = usage.replaceAll("<[^>]+>", ChatColor.LIGHT_PURPLE + "$0" + ChatColor.GREEN);

        return ChatColor.GREEN + usage + " - " + ChatColor.WHITE + desc;
    }
}
