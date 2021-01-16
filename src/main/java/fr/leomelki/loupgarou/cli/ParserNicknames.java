package fr.leomelki.loupgarou.cli;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.leomelki.loupgarou.classes.LGPlayer;
import fr.leomelki.loupgarou.MainLg;

class ParserNicknames extends ParserAbstract {
  protected ParserNicknames(final CommandInterpreter other) {
    super(other);
  }

  /* ========================================================================== */
  /*                             SET NICK TO PLAYER                             */
  /* ========================================================================== */

  protected void processNick(final CommandSender sender, final String[] args) {
    if (args.length < 3) {
      sender.sendMessage("\n§4Erreur: §cCommande incorrecte.");
      sender.sendMessage("§4Essayez §c/lg nick <pseudo_minecraft> <surnom>");
    }

    final String playerName = args[1];
    final Player player = Bukkit.getPlayer(playerName);

    if (player == null) {
      sender.sendMessage(PLAYER_ERROR_PREFIX + playerName + "§c n'existe pas !");
      return;
    }

    final LGPlayer lgp = LGPlayer.thePlayer(player);

    if (lgp.getGame() == null) {
      sender.sendMessage(PLAYER_ERROR_PREFIX + lgp.getName() + "§c n'est pas dans une partie.");
      return;
    }

    final List<String> nicknameTokens = Arrays.asList(Arrays.copyOfRange(args, 2, args.length));
    final String nickname = String.join(" ", nicknameTokens);
    final Player detect = Bukkit.getPlayerExact(nickname);

    if (detect != null) {
      sender.sendMessage("\n§4Erreur : §cCe surnom est déjà le pseudo d'un joueur !");
      return;
    }

    final List<LGPlayer> inGamePlayers = this.instanceMainLg.getCurrentGame().getInGame();

    for (final LGPlayer other : inGamePlayers) {
      if (nickname.equalsIgnoreCase(other.getNick())) {
        sender.sendMessage("\n§4Erreur : §cCe surnom est déjà le surnom d'un joueur !");
        return;
      }
    }

    lgp.setNick(nickname);

    for (final LGPlayer other : inGamePlayers) {
      if (lgp != other) {
        other.getPlayer().hidePlayer(MainLg.getInstance(), lgp.getPlayer());
        other.getPlayer().showPlayer(MainLg.getInstance(), lgp.getPlayer());
      }
    }

    lgp.updatePrefix();
    sender.sendMessage("\n§7§o" + lgp.getName(true) + " s'appellera désormais §8§o" + nickname + "§7§o !");
    MainLg.nicksFile.set(lgp.getPlayer().getUniqueId().toString(), nickname);
    final File f = new File(this.instanceMainLg.getDataFolder(), "nicks.yml");

    try {
      MainLg.nicksFile.save(f);
    } catch (final IOException e) {
      sender.sendMessage("\n§4Erreur : §cImpossible de sauvegarder le surnom");
    }
  }

  /* ========================================================================== */
  /*                                UNNICK PLAYER                               */
  /* ========================================================================== */

  protected void processUnnick(final CommandSender sender, final String[] args) {
    if (args.length != 2) {
      sender.sendMessage("\n§4Erreur: §cCommande incorrecte.");
      sender.sendMessage("§4Essayez §c/lg unnick <pseudo_minecraft>");
      return;
    }

    final String playerName = args[1];
    final Player player = Bukkit.getPlayer(playerName);

    if (player == null) {
      sender.sendMessage(PLAYER_ERROR_PREFIX + playerName + "§c n'existe pas !");
      return;
    }

    final LGPlayer lgp = LGPlayer.thePlayer(player);

    if (lgp.getGame() == null) {
      sender.sendMessage(PLAYER_ERROR_PREFIX + lgp.getName() + "§c n'est pas dans une partie.");
      return;
    }

    final MainLg instance = MainLg.getInstance();
    lgp.setNick(null);

    for (final LGPlayer other : instance.getCurrentGame().getInGame()) {
      if (lgp != other) {
        other.getPlayer().hidePlayer(MainLg.getInstance(), lgp.getPlayer());
        other.getPlayer().showPlayer(MainLg.getInstance(), lgp.getPlayer());
      }
    }

    lgp.updatePrefix();
    sender.sendMessage("\n§7§o" + lgp.getName(true) + " s'appellera désormais §8§o" + lgp.getName(false) + "§7§o !");
    MainLg.nicksFile.set(lgp.getPlayer().getUniqueId().toString(), null);
    final File f = new File(this.instanceMainLg.getDataFolder(), "nicks.yml");

    try {
      MainLg.nicksFile.save(f);
    } catch (final IOException e) {
      sender.sendMessage("\n§4Erreur : §cImpossible de sauvegarder le surnom");
    }
  }
}
