package fr.leomelki.loupgarou.classes;

import java.lang.reflect.Constructor;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import fr.leomelki.loupgarou.MainLg;
import fr.leomelki.loupgarou.roles.Role;
import fr.leomelki.com.comphenix.packetwrapper.WrapperPlayServerUpdateHealth;

import org.bukkit.configuration.file.FileConfiguration;

public class LGRoleDistributor {
  private final LGGame game;
  private final List<LGPlayer> players;
  private final ArrayList<Role> roles = new ArrayList<>();
  private final FileConfiguration config;
  private final Map<String, Constructor<? extends Role>> rolesBuilder;
  private final String roleDistribution;

  public LGRoleDistributor(final LGGame game, final FileConfiguration config, final Map<String, Constructor<? extends Role>> rolesBuilder) {
    this.game = game;
    this.players = game.getInGame();
    this.config = config;
    this.rolesBuilder = rolesBuilder;
    this.roleDistribution = config.getString("roleDistribution");
  }

  private Role instantiateRole(Entry<String, Constructor<? extends Role>> currentRole) {
    try {
      final Role createdRole = currentRole.getValue().newInstance(this.game);
      this.roles.add(createdRole);

      return createdRole;
    } catch (Exception e) {
      System.err.println(e.getMessage());

      throw new RuntimeException("Failed to instantiate role: " + currentRole, e);
    }
  }

  private Role instantiateRole(String forcedRole) {
    try {
      final Role createdRole = this.rolesBuilder.get(forcedRole).newInstance(this.game);
      this.roles.add(createdRole);

      return createdRole;
    } catch (Exception e) {
      System.err.println(e.getMessage());

      throw new RuntimeException("Failed to instantiate role: " + forcedRole, e);
    }
  }

  private void setRoleToPlayer(LGPlayer selected, Role givenRole) {
    givenRole.joinAndDisplayRole(selected);

    WrapperPlayServerUpdateHealth update = new WrapperPlayServerUpdateHealth();
    update.setFood(6);
    update.setFoodSaturation(1);
    update.setHealth(20);
    update.sendPacket(selected.getPlayer());
  }

  public List<Role> assignRoles() {
    if (this.roleDistribution.equals("fixed")) {
      return useFixedAssignation();
    }

    if (this.roleDistribution.equals("random")) {
      return useRandomAssignation();
    }

    throw new RuntimeException("Unsupported roleDistribution: '" + this.roleDistribution + "'");
  }

  private List<Role> useFixedAssignation() {
    final SecureRandom random = new SecureRandom();
    final List<LGPlayer> toGive = new ArrayList<>(players);

    for (Entry<String, Constructor<? extends Role>> currentRole : this.rolesBuilder.entrySet()) {
      if (this.config.getInt("distributionFixed." + currentRole.getKey()) > 0) {
        this.instantiateRole(currentRole);
      }
    }

    for (Role currentRole : this.roles) {
      while (currentRole.getWaitedPlayers() > 0) {
        final int randomized = random.nextInt(toGive.size());
        final LGPlayer selected = toGive.remove(randomized);
        this.setRoleToPlayer(selected, currentRole);
      }
    }

    return this.roles;
  }

  private List<Role> useRandomAssignation() {
    final SecureRandom random = new SecureRandom();
    final ArrayList<LGPlayer> toGive = new ArrayList<>(this.players);

    final int maxPlayers = this.players.size();
    final LGRandomRoleSplit categorySplits = LGRandomRoleSplit.getCategorySplits(maxPlayers, this.config);

    final Map<String, Object> evilWeigths = this.config.getConfigurationSection(MainLg.DISTRIBUTION_RANDOM_KEY + "evilWeigths").getValues(false);
    final LGRandomRolePicker evilRolePicker = new LGRandomRolePicker(game, evilWeigths, this.rolesBuilder);

    for (int i = 0; i < categorySplits.getAmountOfEvil(); i++) {
      final Role pickedRole = evilRolePicker.roll();
      final int randomized = random.nextInt(toGive.size());
      final LGPlayer selected = toGive.remove(randomized);
      this.setRoleToPlayer(selected, pickedRole);
    }

    final Map<String, Object> neutralWeights = this.config.getConfigurationSection(MainLg.DISTRIBUTION_RANDOM_KEY + "neutralWeights").getValues(false);
    final LGRandomRolePicker neutralRolePicker = new LGRandomRolePicker(game, neutralWeights, this.rolesBuilder);
    int amountOfVampires = 0;

    for (int i = 0; i < categorySplits.getAmountOfNeutral(); i++) {
      final Role pickedRole = neutralRolePicker.roll();
      final int randomized = random.nextInt(toGive.size());
      final LGPlayer selected = toGive.remove(randomized);
      this.setRoleToPlayer(selected, pickedRole);

      if (pickedRole.getName().replaceAll("\\§.", "").equals("Vampire")) {
        ++amountOfVampires;
      }
    }

    final Map<String, Object> villagerWeights = this.config.getConfigurationSection(MainLg.DISTRIBUTION_RANDOM_KEY + "villagerWeights").getValues(false);
    final LGRandomRolePicker villagerRolePicker = new LGRandomRolePicker(game, villagerWeights, this.rolesBuilder);

    for (int i = 0; i < categorySplits.getAmountOfVillagers(); i++) {
      final Role pickedRole = (amountOfVampires-- > 0) 
        ? this.instantiateRole("ChasseurDeVampire") 
        : villagerRolePicker.roll();

      final int randomized = random.nextInt(toGive.size());
      final LGPlayer selected = toGive.remove(randomized);
      this.setRoleToPlayer(selected, pickedRole);
    }

    this.roles.addAll(evilRolePicker.getInstantiatedRoles());
    this.roles.addAll(neutralRolePicker.getInstantiatedRoles());
    this.roles.addAll(villagerRolePicker.getInstantiatedRoles());

    return this.roles;
  }
}