package me.zombie_striker.psudocommands.command;

import me.zombie_striker.psudocommands.command.type.CommandType;

public class CMD_PsudoAsConsole extends AbstractCommand{
  @Override
  String getName() {
    return "psudoasconsole";
  }

  @Override
  CommandType getType() {
    return CommandType.PSUDO_AS_CONSOLE;
  }
}
