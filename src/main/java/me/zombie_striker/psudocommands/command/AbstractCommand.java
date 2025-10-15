package me.zombie_striker.psudocommands.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import me.zombie_striker.psudocommands.Utils;
import me.zombie_striker.psudocommands.PsudoCommands;
import me.zombie_striker.psudocommands.command.type.CommandType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommandYamlParser;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public abstract class AbstractCommand {
    abstract String getName();
    abstract CommandType getType();

    public void register(final @NotNull LifecycleEventManager<@NotNull Plugin> manager) {
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commandsRegistrar = event.registrar();
            commandsRegistrar.register(Commands.literal(getName())
                    .requires(context -> context.getSender().hasPermission("psudocommand." + getName()))
                    .executes(ctx -> {
                        ctx.getSource().getSender().sendPlainMessage(Utils.EMPTY_COMMAND_ERROR);
                        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                    })
                    .then(Commands.argument("psudoargs", StringArgumentType.greedyString())
                            .suggests(AbstractCommand::getArgumentSuggestion)
                            .executes(context -> getArgumentExecutes(context, getType()))
                    ).build()
            );
        });
        PsudoCommands.logger().log(Level.INFO, "Registered command " + getName() + " (through Paper Brigadier API).");
    }

    protected static CompletableFuture<Suggestions> getArgumentSuggestion(final @NotNull CommandContext<CommandSourceStack> context,
                                                                          @NotNull SuggestionsBuilder builder) {
        CommandSender baseSender = context.getSource().getSender();
        String[] args = builder.getRemaining().split(" ", -1); // -1 to keep trailing space
        List<String> completion = onTabComplete(baseSender, args);
        if (completion != null) {
            // offset the builder to the next argument to not be stuck at the beginning
            int offset = 0;
            for (int i = 0; i < args.length - 1; i += 1) {
                offset += args[i].length() + 1; // +1 for the space deleted with split
            }
            builder = builder.createOffset(builder.getStart() + offset);

            // no need to check if the completion string starts with the remaining, already filter in onTabComplete
            for (String str : completion) {
                builder.suggest(str);
            }
        }
        return builder.buildFuture();
    }

    protected static List<String> onTabComplete(final @NotNull CommandSender sender, final @NotNull String[] args) {
        List<String> completion = new ArrayList<>();
        int remove = 1;
        if (args.length == remove) {
            // Add all loaded command
            String lastArg = args[remove - 1];
            for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
                for (Command c : PluginCommandYamlParser.parse(plugin)) {
                    for (String alias_ : c.getAliases()) {
                        complete(completion, alias_, lastArg);
                    }
                    complete(completion, c.getName(), lastArg);
                }
            }
        } else if (args.length > remove) {
            // remove first args and copy the other to get the tabComplete (like writing command without psudo)
            String[] newArgs = new String[args.length - remove];
            System.arraycopy(args, remove, newArgs, 0, args.length - remove);
            Command newCommand = Bukkit.getServer().getPluginCommand(args[remove - 1]);
            if (newCommand != null) {
                completion = newCommand.tabComplete(sender, args[remove - 1], newArgs);
            }
        }
        return completion;
    }

    protected static void complete(final @NotNull List<String> completion, final @NotNull String target, final @NotNull String arg) {
        if (target.toLowerCase().startsWith(arg.toLowerCase())) {
            completion.add(target);
        }
    }

    protected static int getArgumentExecutes(final @NotNull CommandContext<CommandSourceStack> context, final @NotNull CommandType commandType) {
        String[] args = StringArgumentType.getString(context, "psudoargs").split(" ");
        CommandSender baseSender = context.getSource().getSender();
        CommandSender sender = context.getSource().getExecutor();
        if (sender == null) {
            sender = baseSender;
        }
        boolean result = onCommand(baseSender, sender, context, commandType, args);
        return result ? 1 : 0;
    }

    protected static boolean onCommand(final @NotNull CommandSender baseSender, final @NotNull CommandSender sender,
                                       final @NotNull CommandContext<CommandSourceStack> context, final @NotNull CommandType type,
                                       @NotNull String[] args) {
        if (args.length == 0) {
            baseSender.sendMessage(Utils.EMPTY_COMMAND_ERROR);
            return false;
        }

        // Remove every space between tags inside a selector
        for (int i = 0; i < args.length; i++) {
            if (Utils.isSelectorStartWithTag(args[i])) {
                args = Utils.combineArgs(args, i, "", "]", "");
                // Because the method combineArgs ignore the last char
                args[i] = args[i] + "]";
            }
        }

        List<StringBuilder> cmds = new ArrayList<>();
        cmds.add(new StringBuilder());
        int nextstep;
        for (int i = 0; i < args.length; i += nextstep) {
            nextstep = 1;
            List<StringBuilder> temps = new ArrayList<>();
            for (StringBuilder cmd : cmds) {

                if (type == CommandType.PSUDO_AS_RAW) {
                    cmd.append(args[i]);
                } else {

                    // Set of 3 relative, world or local coordinates. The first one has to be relative or local ~ ^
                    if (Utils.isRelativeCoord(args[i], true)) {
                        nextstep = 3;
                        if (i + 2 >= args.length || !Utils.isRelativeCoord(args[i + 1], false)
                                || !Utils.isRelativeCoord(args[i + 2], false)) {
                            baseSender.sendMessage(ChatColor.RED + "Please provide three coordinates after \"" + args[i] + "\"");
                            return false;
                        }

                        Location arv;
                        if (args[i].startsWith("^")) {
                            if (!args[i + 1].startsWith("^") || !args[i + 2].startsWith("^")) {
                                baseSender.sendMessage(ChatColor.RED + "Cannot mix world & local coordinates (everything must either use ^ or not)");
                                return false;
                            }
                            arv = Utils.getLocalCoord(Utils.getCoordinate(args[i]),
                                    Utils.getCoordinate(args[i + 1]),
                                    Utils.getCoordinate(args[i + 2]),
                                    context);

                        } else { // if args[i].startWith("~")
                            if (args[i + 1].startsWith("^") || args[i + 2].startsWith("^")) {
                                baseSender.sendMessage(ChatColor.RED + "Cannot mix world & local coordinates (everything must either use ^ or not)");
                                return false;
                            }
                            Location origin = context.getSource().getLocation();
                            arv = Utils.getRelativeCoord(args[i], args[i + 1], args[i + 2], origin);
                        }
                        cmd.append(arv.getX()).append(" ").append(arv.getY()).append(" ").append(arv.getZ());

                        // Targets from the selector
                    } else if (Utils.isSelector(args[i])) {
                        List<Entity> selectedEntities;
                        try {
                            selectedEntities = Bukkit.selectEntities(context.getSource().getSender(), args[i]); // separate "at" and "as" with wrapper
                        } catch (IllegalArgumentException e) {
                            baseSender.sendMessage(ChatColor.RED + e.getMessage());
                            baseSender.sendMessage(ChatColor.RED + "> " + e.getCause());
                            if (e.getCause() != null) baseSender.sendMessage(ChatColor.RED + e.getCause().getMessage());
                            return false;
                        }
                        for (int j = 1; j < selectedEntities.size(); j++) {
                            StringBuilder sb = new StringBuilder(cmd.toString());
                            if (selectedEntities.get(j) == null) {
                                return false;
                            }
                            if (type != CommandType.PSUDO_UUID) {
                                sb.append((selectedEntities.get(j).getCustomName() != null
                                        ? selectedEntities.get(j).getCustomName()
                                        : selectedEntities.get(j).getName()));
                            } else {
                                sb.append(selectedEntities.get(j).getUniqueId());
                            }
                            if (i + 1 < args.length) {
                                sb.append(" ");
                            }
                            temps.add(sb);
                        }
                        if (selectedEntities.isEmpty() || selectedEntities.get(0) == null) {
                            return false;
                        } else {
                            if (type != CommandType.PSUDO_UUID) {
                                cmd.append(selectedEntities.get(0).getCustomName() != null
                                        ? selectedEntities.get(0).getCustomName()
                                        : selectedEntities.get(0).getName());
                            } else {
                                cmd.append(selectedEntities.get(0).getUniqueId());
                            }
                        }

                    } else {
                        cmd.append(args[i]);
                    }
                }

                if (i + 1 < args.length) {
                    cmd.append(" ");
                }
            }
            if (!temps.isEmpty()) {
                cmds.addAll(temps);
                temps.clear();
            }
        }
        boolean atLeastOne = false;
        CommandSender actualSender = type == CommandType.PSUDO_AS_CONSOLE ? Bukkit.getConsoleSender() : (type.useBaseSender() ? baseSender : sender);
        for (StringBuilder cmd : cmds) {
            if (type != CommandType.PSUDO_AS_OP) {
                if (Bukkit.dispatchCommand(actualSender, cmd.toString()))
                    atLeastOne = true;
            } else {
                if (Utils.executeIgnorePerms(actualSender, cmd.toString(), args))
                    atLeastOne = true;
            }
        }
        return atLeastOne;
    }
}
