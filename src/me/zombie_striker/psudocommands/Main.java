package me.zombie_striker.psudocommands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommandYamlParser;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class Main extends JavaPlugin {

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		String commandName = command.getName().toLowerCase();
		boolean psudoAs = commandName.equals("psudoas");
		boolean psudoAsRaw = commandName.equals("psudoasraw");
		boolean psudoUUID = commandName.equals("psudouuid");
		boolean psudo = commandName.equals("psudo");
		if ((psudoUUID && sender.hasPermission("psudocommand.psudouuid"))
				|| (psudo && sender.hasPermission("psudocommand.psudo"))
				|| (psudoAs && sender.hasPermission("psudocommand.psudoas"))
				|| (psudoAsRaw && sender.hasPermission("psudocommand.psudoasraw"))) {
			List<String> completion = new ArrayList<>();
			if ((psudoAs || psudoAsRaw) && args.length == 1) {
				complete(completion, "Console", args[0]);
				for (Player player : Bukkit.getOnlinePlayers()) {
					complete(completion, player.getName(), args[0]);
				}
			} else {
				int remove = (psudoAs || psudoAsRaw) ? 2 : 1;
				if (args.length == remove) {
					// Add all loaded command
					String lastArg = args[remove-1];
					for(Plugin plugin : Bukkit.getPluginManager().getPlugins()){
						for(Command c : PluginCommandYamlParser.parse(plugin)) {
							for (String alias_ : c.getAliases()) {
								complete(completion, alias_, lastArg);
							}
							complete(completion, c.getName(), lastArg);
						}
					}
				} else if (args.length > remove) {
					// remove first args and copy the other to get the tabComplete (like writing command without psudo)
					String[] newArgs = new String[args.length-remove];
					System.arraycopy(args, remove, newArgs, 0, args.length - remove);
					Command newCommand = Bukkit.getServer().getPluginCommand(args[remove-1]);
					if (newCommand != null) {
						completion = newCommand.tabComplete(sender, args[remove-1], newArgs);
					}
				}
			}
			return completion;
		}
		return super.onTabComplete(sender, command, alias, args);
	}

	private static void complete(List<String> completion, String target, String arg) {
		if (target.toLowerCase().startsWith(arg.toLowerCase())) {
			completion.add(target);
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label2, String[] args) {
		String commandName = command.getName().toLowerCase();
		boolean psudoAs = commandName.equals("psudoas");
		boolean psudoAsRaw = commandName.equals("psudoasraw");
		boolean psudoUUID = commandName.equals("psudouuid");
		boolean psudo = commandName.equals("psudo");
		if ((!psudoUUID || !sender.hasPermission("psudocommand.psudouuid"))
				&& (!psudo || !sender.hasPermission("psudocommand.psudo"))
				&& (!psudoAs || !sender.hasPermission("psudocommand.psudoas"))
				&& (!psudoAsRaw || !sender.hasPermission("psudocommand.psudoasraw"))) {
			sender.sendMessage(ChatColor.GRAY + "[PsudoCommands] You don't have permission to use this command.");
			return false;
		}
		boolean atleastOne = false;
		CommandSender[] senders = new CommandSender[1];
		if (args.length <= ((psudoAs || psudoAsRaw) ? 1 : 0)) {
			sender.sendMessage(ChatColor.GRAY + "[PsudoCommands] Please provide a valid "
					+ (((psudoAs || psudoAsRaw) && args.length == 0) ? "player name" : "command") + ".");
			return false;
		}

		// Remove every spaces between tags inside a selector
		for (int i = 0; i < args.length; i++) {
			if (CommandUtils.isSelectorStartWithTag(args[i])) {
				args = CommandUtils.combineArgs(args, i, "", "]", "");
				// Because the method combineArgs remove the begin and end
				// strings it found
				args[i] = args[i] + "]";
			}
		}

		// Get senders (only one if it's not /psudoas or /psudoasraw)
		if (psudoAs || psudoAsRaw) {
			if (args[0].equalsIgnoreCase("Console"))
				senders[0] = Bukkit.getConsoleSender();
			else if (CommandUtils.isSelector(args[0])) {
				senders = CommandUtils.getTargets(sender, args[0]);
			} else {
				senders[0] = Bukkit.getPlayer(args[0]);
			}
		} else {
			senders[0] = sender;
		}
		if (senders == null || (senders.length != 0 && senders[0] == null)) {
			sender.sendMessage(ChatColor.RED + "The sender is null. Choose a valid player or \"Console\"");
			return false;
		} else if (senders.length == 0) {
			return true; // no sender -> the command does nothing at all, so it's working
		}

		for (CommandSender issue : senders) {
			List<StringBuilder> cmds = new ArrayList<>();
			cmds.add(new StringBuilder());
			int step;
			for (int i = ((psudoAs || psudoAsRaw) ? 1 : 0); i < args.length; i += step) {
				step = 1;
				List<StringBuilder> temps = new ArrayList<>();
				for (StringBuilder cmd : cmds) {

					// Set of 3 relative, world or local coordinates. The first one has to be relative or local ~ ^
					if (!psudoAsRaw && CommandUtils.isRelativeCoord(args[i], true)) {
						step = 3;
						if (i + 2 >= args.length || !CommandUtils.isRelativeCoord(args[i+1], false)
												 || !CommandUtils.isRelativeCoord(args[i+2], false)) {
							sender.sendMessage(ChatColor.RED +
								   "Please provide three coordinates after \"" + args[i] + "\"");
							return false;
						}
						Location origin = CommandUtils.getLocation(issue);
						Location arv;
						if(args[i].startsWith("^")) {
							if(!args[i+1].startsWith("^") || !args[i+2].startsWith("^")) {
								sender.sendMessage(ChatColor.RED + "Cannot mix world & local coordinates " +
										"(everything must either use ^ or not)");
								return false;
							}
							arv = CommandUtils.getLocalCoord(args[i], args[i+1], args[i+2], origin);
						} else {
							if(args[i+1].startsWith("^") || args[i+2].startsWith("^")) {
								sender.sendMessage(ChatColor.RED + "Cannot mix world & local coordinates " +
										"(everything must either use ^ or not)");
								return false;
							}
							arv = CommandUtils.getRelativeCoord(args[i], args[i+1], args[i+2], origin);
						}
						cmd.append(arv.getX()).append(" ").append(arv.getY()).append(" ").append(arv.getZ());

					// Targets from the selector
					} else if (!psudoAsRaw && CommandUtils.isSelector(args[i])) {
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
								sb.append(e[j].getUniqueId());
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
								cmd.append(e[0].getUniqueId());
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
}