package me.zombie_striker.psudocommands.command;

import me.zombie_striker.psudocommands.command.type.CommandType;

public class CMD_PsudoAs extends AbstractCommand{
  @Override
  String getName() {
    return "psudoas";
  }

  @Override
  CommandType getType() {
    return CommandType.PSUDO_AS;
  }
}
