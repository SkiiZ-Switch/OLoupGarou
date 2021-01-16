package fr.leomelki.loupgarou.roles;

import java.util.ArrayList;

import org.bukkit.event.EventHandler;

import fr.leomelki.loupgarou.classes.LGGame;
import fr.leomelki.loupgarou.classes.LGPlayer;
import fr.leomelki.loupgarou.classes.LGWinType;
import fr.leomelki.loupgarou.events.LGDayEndEvent;
import fr.leomelki.loupgarou.events.LGEndCheckEvent;
import fr.leomelki.loupgarou.events.LGGameEndEvent;
import fr.leomelki.loupgarou.events.LGPlayerGotKilledEvent;
import fr.leomelki.loupgarou.events.LGVoteEvent;
import fr.leomelki.loupgarou.events.LGPlayerKilledEvent.Reason;

public class RAnge extends Role {
  ArrayList<LGPlayer> winners = new ArrayList<>();
  int night = 1;

  public RAnge(LGGame game) {
    super(game);
  }

  @Override
  public RoleType getType() {
    return RoleType.NEUTRAL;
  }

  @Override
  public RoleWinType getWinType() {
    return RoleWinType.VILLAGE;
  }

  @Override
  public String getName(int amount) {
    final String baseline = this.getName();

    return (amount > 1) ? baseline + "s" : baseline;
  }

  @Override
  public String getName() {
    return "§d§lAnge";
  }

  @Override
  public String getFriendlyName() {
    return "de l'" + getName();
  }

  @Override
  public String getShortDescription() {
    return "Tu gagnes si tu remplis ton objectif";
  }

  @Override
  public String getDescription() {
    return "Tu es §d§lNeutre§f et tu gagnes si tu remplis ton objectif. Ton objectif est d'être éliminé par le village lors du premier vote de jour. Si tu réussis, tu gagnes la partie. Sinon, tu deviens un §a§lVillageois§f.";
  }

  @Override
  public String getTask() {
    return "";
  }

  @Override
  public String getBroadcastedTask() {
    return "";
  }

  @Override
  public int getTimeout() {
    return -1;
  }

  @EventHandler
  public void onVoteStart(LGVoteEvent e) {
    if (e.getGame() == getGame()) {
      night = getGame().getNight();
      vote = true;
      for (LGPlayer lgp : getPlayers())
        if (!lgp.isDead() && lgp.isRoleActive())
          lgp.sendMessage("§9§oFais en sorte que les autres votent contre toi !");
    }
  }

  boolean vote;

  @EventHandler
  public void onDayEnd(LGDayEndEvent e) {
    if (e.getGame() == getGame()) {
      if (!getPlayers().isEmpty() && getGame().getNight() == night + 1 && vote) {
        Role villageois = null;
        for (Role role : getGame().getRoles()) {
          if (role instanceof RVillageois)
            villageois = role;
        }

        if (villageois == null) {
          villageois = new RVillageois(getGame());
          getGame().getRoles().add(villageois);
        }

        for (LGPlayer lgp : getPlayers()) {
          if (lgp.isRoleActive())
            lgp.sendMessage("§4§oTu as échoué, tu deviens §a§l§oVillageois§4§o...");
          lgp.setRole(villageois);
          villageois.join(lgp);
        }

        getPlayers().clear();
        getGame().updateRoleScoreboard();
      }
      vote = false;
    }
  }

  @EventHandler
  public void onDeath(LGPlayerGotKilledEvent e) {
    if (e.getGame() == getGame() && e.getReason() == Reason.VOTE && e.getKilled().getRole() == this
        && getGame().getNight() == night && e.getKilled().isRoleActive())
      winners.add(e.getKilled());
  }

  @EventHandler
  public void onWinCheck(LGEndCheckEvent e) {
    if (e.getGame() == getGame() && !winners.isEmpty())
      e.setWinType(winners.size() == 1 && winners.get(0).getCache().has("inlove") ? LGWinType.COUPLE : LGWinType.ANGE);
  }

  @EventHandler
  public void onWin(LGGameEndEvent e) {
    if (e.getGame() == getGame() && e.getWinType() == LGWinType.ANGE)
      e.getWinners().addAll(winners);
  }
}
