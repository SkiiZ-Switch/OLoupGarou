package fr.leomelki.loupgarou.cli;

import java.util.Collection;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

class ParserConfig extends ParserAbstract {
  protected ParserConfig(CommandInterpreter other) {
    super(other);
  }

  /* ========================================================================== */
  /*                                RELOAD CONFIG                               */
  /* ========================================================================== */

  protected void processReloadConfig(CommandSender sender) {
    sender.sendMessage("\n§aVous avez bien reload la config !");
    sender.sendMessage("§7§oSi vous avez changé les rôles, écriver §8§o/lg joinall§7§o !");
    this.instanceMainLg.loadConfig();
  }

  /* ========================================================================== */
  /*                                RELOAD PACKS                                */
  /* ========================================================================== */

  protected void processReloadPacks(CommandSender sender) {
    final Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();

    for (Player p : onlinePlayers) {
      Bukkit.getPluginManager().callEvent(new PlayerQuitEvent(p, "reloadPacks"));
    }

    for (Player p : onlinePlayers) {
      Bukkit.getPluginManager().callEvent(new PlayerJoinEvent(p, "reloadPacks"));
    }

    sender.sendMessage("\n§aLes packs de resources ont été rechargés par " + onlinePlayers.size() + " joueurs");
  }
}
