package me.zombie_striker.psudocommands.command;

import me.zombie_striker.psudocommands.command.type.CommandType;

public class CMD_Psudo extends AbstractCommand{

    @Override
    String getName() {
        return "psudo";
    }

  @Override
  CommandType getType() {
    return CommandType.PSUDO;
  }
}
