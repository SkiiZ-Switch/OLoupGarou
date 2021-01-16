package fr.leomelki.loupgarou.cli;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import fr.leomelki.loupgarou.MainLg;
import fr.leomelki.loupgarou.classes.LGRandomRoleSplit;

class ParserRolesRandom extends ParserRolesAbstract {
  protected ParserRolesRandom(CommandInterpreter other) {
    super(other);
    this.expectedDistribution = "random";
  }

  protected void processRoles(CommandSender sender, String[] args) {
    final boolean isAuthorized = this.isAuthorized(sender);

    if (args.length == 1) {
      this.displayAvailableRoles(sender);
      return;
    }

    if (args[1].equalsIgnoreCase("players")) {
      if (!isAuthorized) {
        this.denyCommand(sender);
        return;
      }

      this.setPlayerSlots(sender, args);
    }

    if (args[1].equalsIgnoreCase("showAll")) {
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
  /*                              SET PLAYER SLOTS                              */
  /* ========================================================================== */

  private void setPlayerSlots(CommandSender sender, String[] args) {
    final Integer amount = this.parseInteger(args[3]);

    if (amount == null) {
      sender.sendMessage("\n§4Erreur: La valeur §c'" + args[3] + "'§4 n'est pas une quantité valide de joueurs");
    }

    this.setOpenedSlots(amount);
    this.printAmountOfPlayers(sender, amount);
    sender.sendMessage("§7§oSi vous avez fini de changer les rôles, utilisez §8§o/lg joinall§7§o");
  }

  /* ========================================================================== */
  /*                           DISPLAY AVAILABLE ROLES                          */
  /* ========================================================================== */

  @Override
  protected void displayAvailableRoles(CommandSender sender) {
    this.warnWhenRoleDistributionDontMatch(sender, "\n§l§5/!\\ Les valeurs qui suivent ne sont applicables qu'avec");
    
    final int amountOfPlayers = this.getOpenedSlots();
    final FileConfiguration config = this.instanceMainLg.getConfig();
    final LGRandomRoleSplit categorySplits = LGRandomRoleSplit.getCategorySplits(amountOfPlayers, config);

    this.printAmountOfPlayers(sender, amountOfPlayers);
    
    final Map<String, Object> evilWeigths = config.getConfigurationSection(MainLg.DISTRIBUTION_RANDOM_KEY + "evilWeigths").getValues(false);
    final Map<String, Object> neutralWeights = config.getConfigurationSection(MainLg.DISTRIBUTION_RANDOM_KEY + "neutralWeights").getValues(false);
    final Map<String, Object> villagerWeights = config.getConfigurationSection(MainLg.DISTRIBUTION_RANDOM_KEY + "villagerWeights").getValues(false);
    
    this.printCategoryDistribution(sender, evilWeigths, categorySplits.getAmountOfEvil(), "méchants", false);
    this.printCategoryDistribution(sender, neutralWeights, categorySplits.getAmountOfNeutral(), "neutres", false);
    this.printCategoryDistribution(sender, villagerWeights, categorySplits.getAmountOfVillagers(), "villagois", false);
  }

  /* ========================================================================== */
  /*                              DISPLAY ALL ROLES                             */
  /* ========================================================================== */

  @Override
  protected void displayAllRoles(CommandSender sender) {
    this.warnWhenRoleDistributionDontMatch(sender, "\n§l§5/!\\ Les valeurs qui suivent ne sont applicables qu'avec");
    
    final int amountOfPlayers = this.getOpenedSlots();
    final FileConfiguration config = this.instanceMainLg.getConfig();
    final LGRandomRoleSplit categorySplits = LGRandomRoleSplit.getCategorySplits(amountOfPlayers, config);

    this.printAmountOfPlayers(sender, amountOfPlayers);
    
    final Map<String, Object> evilWeigths = config.getConfigurationSection(MainLg.DISTRIBUTION_RANDOM_KEY + "evilWeigths").getValues(false);
    final Map<String, Object> neutralWeights = config.getConfigurationSection(MainLg.DISTRIBUTION_RANDOM_KEY + "neutralWeights").getValues(false);
    final Map<String, Object> villagerWeights = config.getConfigurationSection(MainLg.DISTRIBUTION_RANDOM_KEY + "villagerWeights").getValues(false);
    
    this.printCategoryDistribution(sender, evilWeigths, categorySplits.getAmountOfEvil(), "méchants", true);
    this.printCategoryDistribution(sender, neutralWeights, categorySplits.getAmountOfNeutral(), "neutres", true);
    this.printCategoryDistribution(sender, villagerWeights, categorySplits.getAmountOfVillagers(), "villagois", true);
  }

  /* ========================================================================== */
  /*                             SET ROLE AVAILABILITY                          */
  /* ========================================================================== */

  @Override
  protected void setRoleAvailability(CommandSender sender, String[] args) {
    sender.sendMessage("\n§4NOT IMPLEMENTED YET");
  }

  /* ========================================================================== */
  /*                                UTILITY METHODS                            */
  /* ========================================================================== */

  private int getOpenedSlots() {
    final String amountOfPlayersKey = MainLg.DISTRIBUTION_RANDOM_KEY + "amountOfPlayers";

    return this.instanceMainLg.getConfig().getInt(amountOfPlayersKey);
  }

  private void setOpenedSlots(final int amount) {
    final String amountOfPlayersKey = MainLg.DISTRIBUTION_RANDOM_KEY + "amountOfPlayers";

    this.instanceMainLg.getConfig().set(amountOfPlayersKey, amount);
  }

  private void printAmountOfPlayers(final CommandSender sender, final int amountOfPlayers) {
    sender.sendMessage("\n§6Il y aura §e" + amountOfPlayers + "§6 joueurs au total");
  }

  private void printCategoryDistribution(final CommandSender sender, final Map<String, Object> weights, final double amount, final String type, final boolean displayZeroWeightEntries) {
    final boolean categoryShouldBeDisplayed = displayZeroWeightEntries || (amount > 0);
    
    if (!categoryShouldBeDisplayed) {
      return;
    }
    
    final int totalWeights = weights.values().stream().map(e -> (int)e).reduce(0, Integer::sum);
    final List<Entry<String, Object>> sortedEntries = weights.entrySet().stream().sorted((o1, o2) -> ((Integer)o2.getValue()).compareTo((Integer)o1.getValue())).collect(Collectors.toList());

    sender.sendMessage("\n§e" + (int)amount + "§6 " + type + " §7(poids total: " + totalWeights + ")");

    for (Entry<String, Object> current : sortedEntries) {
      final String role = current.getKey();
      final int currentWeight = (int) current.getValue();
      final boolean entryShouldBeDisplayed = displayZeroWeightEntries || (currentWeight > 0);

      if (entryShouldBeDisplayed) {
        final String percentage = this.extractPercentage(currentWeight, totalWeights);

        sender.sendMessage("§7  - §6" + role + "§e " + percentage + " §7(poids: " + currentWeight + ")");
      }
    }
  }

  private String extractPercentage(final int amount, final int total) {
    final double votePercentage = ((double) amount / total) * 100;

    return String.format("%.0f%%", votePercentage);
  }
}