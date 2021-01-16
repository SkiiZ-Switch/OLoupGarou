package fr.leomelki.loupgarou.cli;

import org.bukkit.command.CommandSender;

import fr.leomelki.loupgarou.classes.LGGame;
import fr.leomelki.loupgarou.classes.LGPlayer;

class ParserRounds extends ParserAbstract {
  protected ParserRounds(CommandInterpreter other) {
    super(other);
  }

  /* ========================================================================== */
  /*                               FORCE NEXT DAY                               */
  /* ========================================================================== */

  protected void processNextDay(CommandSender sender) {
    final LGGame currentGame = this.instanceMainLg.getCurrentGame();

    if (currentGame == null) {
      sender.sendMessage("\n§aAucune partie n'est active");
      return;
    }

    currentGame.broadcastMessage("§2§lLe passage à la prochaine journée a été forcé !");
    for (LGPlayer lgp : currentGame.getInGame()) {
      lgp.stopChoosing();
    }

    currentGame.cancelWait();
    currentGame.endNight();
  }

  /* ========================================================================== */
  /*                              FORCE NEXT NIGHT                              */
  /* ========================================================================== */

  protected void processNextNight(CommandSender sender) {
    final LGGame currentGame = this.instanceMainLg.getCurrentGame();

    if (currentGame == null) {
      sender.sendMessage("\n§aAucune partie n'est active");
      return;
    }

    sender.sendMessage("§aVous êtes passé à la prochaine nuit");
    currentGame.broadcastMessage("§2§lLe passage à la prochaine nuit a été forcé !");

    for (LGPlayer lgp : currentGame.getInGame()) {
      lgp.stopChoosing();
    }

    currentGame.cancelWait();
    currentGame.nextNight();
  }
}
