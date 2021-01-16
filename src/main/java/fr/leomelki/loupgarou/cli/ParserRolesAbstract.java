package fr.leomelki.loupgarou.cli;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

abstract class ParserRolesAbstract extends ParserAbstract {
  protected final String roleDistribution;
  protected String expectedDistribution;

  protected ParserRolesAbstract(CommandInterpreter other) {
    super(other);

    final FileConfiguration config = this.instanceMainLg.getConfig();

    this.expectedDistribution = "REPLACE_ME";
    this.roleDistribution = config.getString("roleDistribution");
  }

  protected abstract void processRoles(CommandSender sender, String[] args);
  protected abstract void displayAvailableRoles(CommandSender sender);
  protected abstract void displayAllRoles(CommandSender sender);
  protected abstract void setRoleAvailability(CommandSender sender, String[] args);

  protected void warnWhenRoleDistributionDontMatch(final CommandSender sender, final String prefix) {
    if (this.roleDistribution.equals(this.expectedDistribution)) {
      return;
    }

    sender.sendMessage(prefix + " §b'roleDistribution: " + expectedDistribution + "'§5 dans config.yml.\n/!\\ Actuellement le mode de distribution est: §b'roleDistribution: " + roleDistribution + "'§5\n");
  }

  protected String parseRoleKey(String raw) {
    try {
      final int roleID = Integer.parseInt(raw);
      final Object[] array = this.instanceMainLg.getRolesBuilder().keySet().toArray();

      return (array.length > roleID) ? (String) array[roleID] : null;
    } catch (NumberFormatException e) {
      return raw;
    }
  }

  protected String getRoleName(String raw) {
    final String rawRoleName = this.parseRoleKey(raw);

    return (rawRoleName != null)
        ? this.instanceMainLg.getRolesBuilder().keySet().stream().filter(e -> e.equalsIgnoreCase(rawRoleName)).findAny()
            .orElse(null)
        : null;
  }
}