package fr.leomelki.loupgarou.cli;

import org.bukkit.command.CommandSender;

import fr.leomelki.loupgarou.MainLg;

class ParserRolesFixed extends ParserRolesAbstract {
  protected ParserRolesFixed(CommandInterpreter other) {
    super(other);
    this.expectedDistribution = "fixed";
  }

  protected void processRoles(CommandSender sender, String[] args) {
    final boolean isAuthorized = this.isAuthorized(sender);

    if (args.length == 1) {
      this.displayAvailableRoles(sender);
      return;
    }

    if (args[1].equalsIgnoreCase("list")) {
      this.displayAllRoles(sender);
      return;
    }

    if (args[1].equalsIgnoreCase("set")) {
      if (!isAuthorized) {
        this.denyCommand(sender);
        return;
      }

      this.setRoleAvailability(sender, args);
      return;
    }

    sender.sendMessage("\n§4Erreur: §cCommande incorrecte.");
    sender.sendMessage("§4Essayez §c/lg roles§4 ou §c/lg roles list§4 ou §c/lg roles set <role_id/role_name> <nombre>§4");
  }

  /* ========================================================================== */
  /*                           DISPLAY AVAILABLE ROLES                          */
  /* ========================================================================== */

  @Override
  protected void displayAvailableRoles(CommandSender sender) {
    this.warnWhenRoleDistributionDontMatch(sender, "\n§l§5/!\\ Les valeurs qui suivent ne sont applicables qu'avec");

    sender.sendMessage("\n§7Voici la liste des rôles:");

    for (String role : this.instanceMainLg.getRolesBuilder().keySet()) {
      final int openedSlots = this.getOpenedSlots(role);

      if (openedSlots > 0) {
        sender.sendMessage("  §e- §6" + role + " §e: " + openedSlots);
      }
    }
  }

  /* ========================================================================== */
  /*                              DISPLAY ALL ROLES                             */
  /* ========================================================================== */

  @Override
  protected void displayAllRoles(CommandSender sender) {
    this.warnWhenRoleDistributionDontMatch(sender, "\n§l§5/!\\ Les valeurs qui suivent ne sont applicables qu'avec");

    int index = 0;
    sender.sendMessage("\n§7roVoici la liste complète des rôles:");

    for (String role : this.instanceMainLg.getRolesBuilder().keySet()) {
      final int openedSlots = this.getOpenedSlots(role);

      sender.sendMessage("  §e- " + (index++) + " - §6" + role + " §e> " + openedSlots);
    }

    sender.sendMessage(
        "\n§7Écrivez §8§o/lg roles set <role_id/role_name> <nombre>§7 pour définir le nombre de joueurs qui devrons avoir ce rôle.");
  }

  /* ========================================================================== */
  /*                             SET ROLE AVAILABILITY                          */
  /* ========================================================================== */

  @Override
  protected void setRoleAvailability(CommandSender sender, String[] args) {
    if (args.length != 4) {
      sender.sendMessage("\n§4Erreur: §cCommande incorrecte.");
      sender.sendMessage("§4Essayez §c/lg roles set <role_id/role_name> <nombre>§4");
      return;
    }

    this.warnWhenRoleDistributionDontMatch(sender, "\n§l§5/!\\ Ces valeurs vont être sauvegardées mais ne seront utilisées qu'avec");
    final String roleName = this.getRoleName(args[2]);

    if (roleName == null) {
      sender.sendMessage("\n§4Erreur: Le rôle §c'" + args[2] + "'§4 n'existe pas");
    }

    final Integer amount = this.parseInteger(args[3]);

    if (amount == null) {
      sender.sendMessage("\n§4Erreur: La valeur §c'" + args[3] + "'§4 n'est pas une quantité valide de joueurs");
    }

    this.setOpenedSlots(roleName, amount);

    sender.sendMessage("\n§6Il y aura §e " + amount + " §6" + roleName);
    this.instanceMainLg.saveConfig();
    this.instanceMainLg.loadConfig();
    sender.sendMessage("§7§oSi vous avez fini de changer les rôles, utilisez §8§o/lg joinall§7§o");
  }

  /* ========================================================================== */
  /*                                UTILITY METHODS                            */
  /* ========================================================================== */

  private int getOpenedSlots(final String role) {
    final String roleKey = MainLg.DISTRIBUTION_FIXED_KEY + role;

    return this.instanceMainLg.getConfig().getInt(roleKey);
  }

  private void setOpenedSlots(final String role, final int amount) {
    final String roleKey = MainLg.DISTRIBUTION_FIXED_KEY + role;

    this.instanceMainLg.getConfig().set(roleKey, amount);
  }
}