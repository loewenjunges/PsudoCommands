package me.zombie_striker.psudocommands.command;

import me.zombie_striker.psudocommands.command.type.CommandType;

public class CMD_PsudoUuid extends AbstractCommand {
  @Override
  String getName() {
    return "psudouuid";
  }

  @Override
  CommandType getType() {
    return CommandType.PSUDO_UUID;
  }
}
