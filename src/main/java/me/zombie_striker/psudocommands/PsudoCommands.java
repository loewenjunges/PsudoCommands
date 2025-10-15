package me.zombie_striker.psudocommands;

import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import me.zombie_striker.psudocommands.command.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

public class PsudoCommands extends JavaPlugin {

    private static PsudoCommands instance;

    public void onEnable() {
        instance = this;

        final LifecycleEventManager<@NotNull Plugin> manager = getLifecycleManager();

        new CMD_Psudo().register(manager);
        new CMD_PsudoAs().register(manager);
        new CMD_PsudoAsConsole().register(manager);
        new CMD_PsudoAsOp().register(manager);
        new CMD_PsudoAsRaw().register(manager);
        new CMD_PsudoUuid().register(manager);
    }

    public static PsudoCommands getInstance() {
        return instance;
    }

    public static Logger logger() {
        return getInstance().getLogger();
    }
}
