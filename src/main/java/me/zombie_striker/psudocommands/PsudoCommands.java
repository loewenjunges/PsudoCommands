package me.zombie_striker.psudocommands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.logging.Level;

public class PsudoCommands extends JavaPlugin {

    public void onEnable() {
        PsudoCommandExecutor executor = new PsudoCommandExecutor(this);

        PluginCommand[] commands = new PluginCommand[]{ getCommand("psudo"), getCommand("psudouuid"),
                getCommand("psudoas"), getCommand("psudoasraw"),
                getCommand("psudoasop"), getCommand("psudoasconsole") };

        for (PluginCommand command : commands) {
            command.setExecutor(executor);
            command.setTabCompleter(executor);
            getLogger().log(Level.INFO, "Registered command " + command.getName() + " for vanilla usage.");
        }

        try {
            CommandDispatcher<Object> dispatcher = PsudoReflection.getCommandDispatcher();
            for (PluginCommand command : commands) {
                dispatcher.register(buildCommand(executor, command, PsudoCommandExecutor.PsudoCommandType.getType(command)));
                getLogger().log(Level.INFO, "Registered command " + command.getName() + " for /execute (through Brigadier).");
            }
        } catch (ExceptionInInitializerError e) {
            getLogger().log(Level.SEVERE, "Could not load reflection ! Is your version (" + Bukkit.getVersion() + ") supported ? -> " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Every <Object> is actually a <CommandListenerWrapper>
    private static LiteralArgumentBuilder<Object> buildCommand(PsudoCommandExecutor executor, PluginCommand command, PsudoCommandExecutor.PsudoCommandType commandType) {
        RequiredArgumentBuilder<Object, ?> argumentBuilder = RequiredArgumentBuilder.argument("arguments", StringArgumentType.greedyString());
        argumentBuilder.suggests((context, builder) -> { // SuggestionProvider<CommandListenerWrapper>
                    CommandSender baseSender = PsudoReflection.getBukkitBasedSender(context.getSource());
                    String[] args = builder.getRemaining().split(" ", -1); // -1 to keep trailing space
                    List<String> completion = executor.onTabComplete(baseSender, command, null, args);
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
                });
        argumentBuilder.executes(context -> {
                    String[] args = StringArgumentType.getString(context, "arguments").split(" ");
                    Object source = context.getSource();
                    CommandSender baseSender = PsudoReflection.getBukkitBasedSender(source);
                    CommandSender sender = PsudoReflection.getBukkitSender(source);
                    if (sender == null) {
                        sender = baseSender;
                    }
                    boolean result = executor.onCommand(baseSender, sender, source, commandType, args);
                    return result ? 1 : 0;
                });

        LiteralArgumentBuilder<Object> builder = LiteralArgumentBuilder.literal(command.getName());
        builder.then(argumentBuilder);
        return builder;
    }
}