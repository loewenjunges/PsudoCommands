package me.zombie_striker.psudocommands.command.type;

import org.bukkit.command.Command;

public enum CommandType {

  PSUDO,
  PSUDO_UUID,
  PSUDO_AS,
  PSUDO_AS_RAW,
  PSUDO_AS_OP,
  PSUDO_AS_CONSOLE;

  public boolean useBaseSender() {
    return this == PSUDO || this == PSUDO_UUID;
  }

  public static CommandType getType(Command command) {
    switch (command.getName().toLowerCase()) {
      case "psudo":
        return PSUDO;
      case "psudoas":
        return PSUDO_AS;
      case "psudoasraw":
        return PSUDO_AS_RAW;
      case "psudouuid":
        return PSUDO_UUID;
      case "psudoasop":
        return PSUDO_AS_OP;
      case "psudoasconsole":
        return PSUDO_AS_CONSOLE;
    }
    return null;
  }
}
