package fr.leomelki.loupgarou.classes;

import java.lang.reflect.Constructor;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import fr.leomelki.loupgarou.roles.Role;

public class LGRandomRolePicker {
  private final LGGame game;
  private final Map<String, Constructor<? extends Role>> rolesBuilder;
  private final Map<String, Object> weights;
  private final Map<String, Role> alreadyInstantiatedRoles = new HashMap<>();
  private final Map<Integer, String> valueMapping = new HashMap<>();
  private int totalWeights = 0;

  public LGRandomRolePicker(final LGGame game, final Map<String, Object> weigths, final Map<String, Constructor<? extends Role>> rolesBuilder) {
    this.game = game;
    this.rolesBuilder = rolesBuilder;
    this.weights = weigths;

    for (Entry<String, Object> currentEntry : weigths.entrySet()) {
      final int intermediaryTotal = this.totalWeights + (int) currentEntry.getValue();
      this.totalWeights = intermediaryTotal;
      this.valueMapping.put(intermediaryTotal, currentEntry.getKey());
    }
  }

  public Role roll() {
    final SecureRandom random = new SecureRandom();
    final int seed = random.nextInt(this.totalWeights);
    final Entry<Integer, String> matchingEntry = this.valueMapping.entrySet().stream().sorted(Entry.comparingByKey())
        .filter(elem -> {
          if (seed > elem.getKey())
            return false;

          return ((int) this.weights.get(elem.getValue()) > 0);
        }).findFirst().orElse(null);

    if (matchingEntry == null) {
      throw new RuntimeException("Failed to find a matching entry for seed:" + seed + " in RandomRolePicker");
    }

    final String roleKey = matchingEntry.getValue();

    if (!this.alreadyInstantiatedRoles.containsKey(roleKey)) {
      try {
        this.alreadyInstantiatedRoles.put(roleKey, this.rolesBuilder.get(roleKey).newInstance(this.game));
      } catch (Exception e) {
        throw new RuntimeException("Failed to instantiate role " + roleKey, e);
      }
    }

    return this.alreadyInstantiatedRoles.get(roleKey);
  }

  public List<Role> getInstantiatedRoles() {
    return this.alreadyInstantiatedRoles.entrySet().stream().map(Entry::getValue).collect(Collectors.toList());
  }
}