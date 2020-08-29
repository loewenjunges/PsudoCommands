package me.zombie_striker.psudocommands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class Main extends JavaPlugin {

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (command.getName().equalsIgnoreCase("psudoAs")) {
			List<String> names = new ArrayList<>();
			names.add("Console");
			for (Player player : Bukkit.getOnlinePlayers())
				names.add(player.getName());
			return names;
		}
		return super.onTabComplete(sender, command, alias, args);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label2, String[] args) {
		boolean psudoAs = command.getName().equalsIgnoreCase("psudoas");
		boolean psudoUUID = command.getName().equals("psudoUUID");
		boolean psudo = command.getName().equals("psudo");
		if ((psudoUUID && sender.hasPermission("psudocommand.psudouuid"))
				|| (psudo && sender.hasPermission("psudocommand.psudo"))
				|| (psudoAs && sender.hasPermission("psudocommand.psudoas"))) {
			boolean atleastOne = false;
			CommandSender[] senders = new CommandSender[1];
			if (args.length <= (psudoAs ? 1 : 0)) {
				//sender.sendMessage(ChatColor.GRAY + "[PsudoCommands] Please provide a valid "
				//		+ ((psudoAs && args.length == 0) ? "player name" : "command") + ".");
				return false;
			}
			if (psudoAs) {
				if (args[0].equalsIgnoreCase("Console"))
					senders[0] = Bukkit.getConsoleSender();
				else if (args[0].contains("@")) {
					senders = CommandUtils.getTargets(sender, args[0]);
				} else {
					senders[0] = Bukkit.getPlayer(args[0]);
				}
			} else {
				senders[0] = sender;
			}
			if (senders == null || senders.length == 0 || senders[0] == null) {
				sender.sendMessage(ChatColor.RED + "The sender is null. Choose a valid player or \"Console\"");
				return false;
			}
			for (CommandSender issue : senders) {
				List<StringBuilder> cmds = new ArrayList<>();
				cmds.add(new StringBuilder());
				int step;
				for (int i = (psudoAs ? 1 : 0); i < args.length; i += step) {
					step = 1;
					List<StringBuilder> temps = new ArrayList<>();
					for (StringBuilder cmd : cmds) {
						if (CommandUtils.isRelativeCoord(args[i], true)) {
							step = 3;
							if (i + 2 >= args.length || !CommandUtils.isRelativeCoord(args[i+1], false)
									                 || !CommandUtils.isRelativeCoord(args[i+2], false)) {
								sender.sendMessage(ChatColor.RED +
									   "Please provide three coordinates after \"" + args[i] + "\"");
								return false;
							}
							Location origin = ((Entity) issue).getLocation();
							double[] coord;
							if(args[i].startsWith("^")) {
								if(!args[i+1].startsWith("^") || !args[i+2].startsWith("^")) {
									sender.sendMessage(ChatColor.RED + "Cannot mix world & local coordinates " +
											"(everything must either use ^ or not)");
									return false;
								}
								coord = CommandUtils.getLocalCoord(args[i], args[i+1], args[i+2], origin);
							} else {
								if(args[i+1].startsWith("^") || args[i+2].startsWith("^")) {
									sender.sendMessage(ChatColor.RED + "Cannot mix world & local coordinates " +
											"(everything must either use ^ or not)");
									return false;
								}
								coord = CommandUtils.getRelativeCoord(args[i], args[i+1], args[i+2], origin);
							}
							cmd.append(coord[0]).append(" ").append(coord[1]).append(" ").append(coord[2]);
						} else if (args[i].startsWith("@")) {
							Entity[] e = CommandUtils.getTargets(issue, args[i]);
							if (e == null)
								continue;
							boolean works = true;
							for (int j = 1; j < e.length; j++) {
								StringBuilder sb = new StringBuilder(cmd.toString());
								if (e[j] == null) {
									works = false;
									break;
								}
								if (psudo || psudoAs) {
									sb.append((e[j].getCustomName() != null ? e[j].getCustomName() : e[j].getName()));
								} else if (psudoUUID) {
									sb.append(e[j].getUniqueId().toString());
								}
								if (i + 1 < args.length) {
									sb.append(" ");
								}
								temps.add(sb);
							}
							if (!works)
								return false;
							if (e.length == 0 || e[0] == null) {
								return false;
							} else {
								if (psudo || psudoAs) {
									cmd.append(e[0].getCustomName() != null ? e[0].getCustomName() : e[0].getName());
								} else if (psudoUUID) {
									cmd.append(e[0].getUniqueId().toString());
								}
							}
						} else {
							cmd.append(args[i]);
						}
						if (i + 1 < args.length) {
							cmd.append(" ");
						}
					}
					if (temps.size() > 0) {
						cmds.addAll(temps);
						temps.clear();
					}
				}
				for (StringBuilder cmd : cmds) {
					if (Bukkit.dispatchCommand(issue, cmd.toString()))
						atleastOne = true;
				}
			}
			return atleastOne;
		}
		return false;
	}
}
