package me.zombie_striker.psudocommands.command;

import me.zombie_striker.psudocommands.command.type.CommandType;

public class CMD_PsudoAsOp extends AbstractCommand {
  @Override
  String getName() {
    return "psudoasop";
  }

  @Override
  CommandType getType() {
    return CommandType.PSUDO_AS_OP;
  }
}
