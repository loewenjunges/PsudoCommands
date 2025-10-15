package me.zombie_striker.psudocommands.command;

import me.zombie_striker.psudocommands.command.type.CommandType;

public class CMD_PsudoAsRaw extends AbstractCommand{
  @Override
  String getName() {
    return "psudoasraw";
  }

  @Override
  CommandType getType() {
    return CommandType.PSUDO_AS_RAW;
  }
}
