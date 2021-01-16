package fr.leomelki.loupgarou.cli;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.command.CommandSender;

import fr.leomelki.loupgarou.MainLg;
import fr.leomelki.loupgarou.roles.Role;

public class CommandInterpreter extends ParserAbstract {
  public CommandInterpreter(MainLg instance) {
    super(instance);
  }

  /* ========================================================================== */
  /*                               PROCESS COMMAND                              */
  /* ========================================================================== */

  public boolean dispatch(CommandSender sender, String label, String[] args) {
    if (!label.equalsIgnoreCase("lg")) {
      return false;
    }

    this.process(sender, args);

    return true;
  }

  private void process(CommandSender sender, String[] args) {
    if (args[0].equalsIgnoreCase("roles")) {
      (new ParserRolesFixed(this)).processRoles(sender, args);
      return;
    }

    if (args[0].equalsIgnoreCase("random")) {
      (new ParserRolesRandom(this)).processRoles(sender, args);
      return;
    }

    // The rest of the commands require admin permissions
    if (!isAuthorized(sender)) {
      denyCommand(sender);
      return;
    }

    if (args[0].equalsIgnoreCase("joinAll")) {
      (new ParserGame(this)).processJoinAll(sender);
      return;
    }

    if (args[0].equalsIgnoreCase("start")) {
      (new ParserGame(this)).processStartGame(sender, args);
      return;
    }

    if (args[0].equalsIgnoreCase("nextDay")) {
      (new ParserRounds(this)).processNextDay(sender);
      return;
    }

    if (args[0].equalsIgnoreCase("nextNight")) {
      (new ParserRounds(this)).processNextNight(sender);
      return;
    }

    if (args[0].equalsIgnoreCase("end")) {
      (new ParserGame(this)).processEndGame(sender, args);
      return;
    }

    if (args[0].equalsIgnoreCase("addSpawn")) {
      (new ParserSpawnpoints(this)).processSpawn(sender);
      return;
    }

    if (args[0].equalsIgnoreCase("reloadConfig")) {
      (new ParserConfig(this)).processReloadConfig(sender);
      return;
    }

    if (args[0].equalsIgnoreCase("reloadPacks")) {
      (new ParserConfig(this)).processReloadPacks(sender);
      return;
    }

    if (args[0].equalsIgnoreCase("nick")) {
      (new ParserNicknames(this)).processNick(sender, args);
      return;
    }

    if (args[0].equalsIgnoreCase("unnick")) {
      (new ParserNicknames(this)).processUnnick(sender, args);
      return;
    }

    sender.sendMessage("§4Erreur: §cCommande incorrecte.");
    sender.sendMessage(
        "§4Essayez /lg §caddSpawn/end/start/nextNight/nextDay/reloadConfig/roles/reloadPacks/joinAll/nick/unnick");
  }

  /* ========================================================================== */
  /*                              GET AUTOCOMPLETE                              */
  /* ========================================================================== */

  public List<String> getAutocomplete(CommandSender sender, String[] args) {
    if (args.length == 0) {
      return new ArrayList<>(0); 
    }

    final boolean isAuthorized = this.isAuthorized(sender);

    if (args.length == 1) {
      return (isAuthorized)
        ? this.getStartingList(args[0], "addSpawn", "end", "joinAll", "nextDay", "nextNight", "nick", "random", "reloadConfig", "reloadPacks", "roles", "start", "unnick")
        : this.getStartingList(args[0], "random", "roles");
    }

    if (args.length == 2) {
      if (args[0].equalsIgnoreCase("roles")) {
        return (isAuthorized)
          ? this.getStartingList(args[1], "list", "set")
          : this.getStartingList(args[1], "list");
      }

      if (args[0].equalsIgnoreCase("random")) {
        return (isAuthorized) 
          ? this.getStartingList(args[1], "players", "set", "showAll")
          : this.getStartingList(args[1], "showAll");
      }

      return new ArrayList<>(0);  
    }

    if (args.length == 3) {
      final HashMap<String, Constructor<? extends Role>> rolesBuilder = this.instanceMainLg.getRolesBuilder();

      return (isAuthorized) 
        ? getStartingList(args[2], rolesBuilder.keySet().toArray(new String[rolesBuilder.size()]))
        : new ArrayList<>(0);
    }

    if (args.length == 4) {
      return (isAuthorized) 
        ? Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7", "8", "9")
        : new ArrayList<>(0);
    }

		return new ArrayList<>(0);
  }

  private List<String> getStartingList(final String startsWith, String... options) {
    final List<String> results = Arrays.asList(options);

    if (startsWith.length() == 0) {
      return results;
    }

    final String lowercaseStartsWith = startsWith.toLowerCase();

    return results.stream().filter(e -> e.toLowerCase().startsWith(lowercaseStartsWith)).collect(Collectors.toList());
	}
}
