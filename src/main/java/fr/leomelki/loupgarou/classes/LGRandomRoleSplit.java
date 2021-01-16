package fr.leomelki.loupgarou.classes;

import java.util.Map;

import org.bukkit.configuration.file.FileConfiguration;

import fr.leomelki.loupgarou.MainLg;
import lombok.Getter;

public class LGRandomRoleSplit {
  @Getter private final double amountOfEvil;
  @Getter private final double amountOfNeutral;
  @Getter private final double amountOfVillagers;
  
  private LGRandomRoleSplit(final double amountOfEvil, final double amountOfNeutral, final double amountOfVillagers) {
    this.amountOfEvil = amountOfEvil;
    this.amountOfNeutral = amountOfNeutral;
    this.amountOfVillagers = amountOfVillagers;
  }

  public static LGRandomRoleSplit getCategorySplits(final int maxPlayers, final FileConfiguration config) {
    final Map<String, Object> categoryWeigths = config.getConfigurationSection(MainLg.DISTRIBUTION_RANDOM_KEY + "categoryWeights").getValues(false);
    
    final int evilRolesWeigth = (int) categoryWeigths.get("evilRoles");
    final int neutralRolesWeight = (int) categoryWeigths.get("neutralRoles");
    final int villagerRolesWeight = (int) categoryWeigths.get("villagerRoles");
    
    final int totalRolesWeight = evilRolesWeigth + neutralRolesWeight + villagerRolesWeight;
    final double amountOfEvil = Math.floor((double) (maxPlayers * evilRolesWeigth) / totalRolesWeight);
    final double amountOfNeutral = Math.ceil((double) (maxPlayers * neutralRolesWeight) / totalRolesWeight);
    final double amountOfVillagers = maxPlayers - (amountOfEvil + amountOfNeutral);
    
    return new LGRandomRoleSplit(amountOfEvil, amountOfNeutral, amountOfVillagers);
  }
}