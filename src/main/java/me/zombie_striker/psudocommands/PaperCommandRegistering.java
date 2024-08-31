package me.zombie_striker.psudocommands;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;

import java.util.logging.Level;

public class PaperCommandRegistering {
    public static void registerPaperBrigadierCommand(Plugin plugin, PsudoCommandExecutor executor, PluginCommand[] commands) {
        LifecycleEventManager<Plugin> manager = plugin.getLifecycleManager();
        for (PluginCommand command : commands) {
            manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
                final Commands commandsRegistrar = event.registrar();
                commandsRegistrar.register(Commands.literal(command.getName())
                        .executes(ctx -> {
                            ctx.getSource().getSender().sendPlainMessage(CommandUtils.EMPTY_COMMAND_ERROR);
                            return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                        })
                        .then(Commands.argument("psudoargs", StringArgumentType.greedyString())
                                .suggests((context, builder) -> CommandUtils.getArgumentSuggestion(context, builder, executor, command))
                                .executes(context -> CommandUtils.getArgumentExecutes(context, executor, PsudoCommandExecutor.PsudoCommandType.getType(command)))
                        ).build()
                );
            });
            plugin.getLogger().log(Level.INFO, "Registered command " + command.getName() + " (through Paper Brigadier API).");
        }
    }
}
