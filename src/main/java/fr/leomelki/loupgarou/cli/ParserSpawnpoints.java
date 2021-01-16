package fr.leomelki.loupgarou.cli;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

class ParserSpawnpoints extends ParserAbstract {
  protected ParserSpawnpoints(CommandInterpreter other) {
    super(other);
  }

  /* ========================================================================== */
  /*                               ADD SPAWNPOINT                               */
  /* ========================================================================== */

  protected void processSpawn(CommandSender sender) {
    @SuppressWarnings("unchecked")
    final List<Object> list = (List<Object>) this.instanceMainLg.getConfig().getList("spawns");
    final Player player = (Player) sender;
    final Location loc = player.getLocation();

    list.add(Arrays.asList((double) loc.getBlockX(), loc.getY(), (double) loc.getBlockZ(), (double) loc.getYaw(),
        (double) loc.getPitch()));

    this.instanceMainLg.saveConfig();
    this.instanceMainLg.loadConfig();

    sender.sendMessage("\n§aLa position a bien été ajoutée !");
  }
}
