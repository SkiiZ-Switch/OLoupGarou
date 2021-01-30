package fr.leomelki.loupgarou.roles;

import java.util.Arrays;
import java.util.List;

import org.bukkit.event.EventHandler;

import fr.leomelki.loupgarou.classes.LGGame;
import fr.leomelki.loupgarou.classes.LGPlayer;
import fr.leomelki.loupgarou.classes.LGPlayer.LGChooseCallback;
import fr.leomelki.loupgarou.events.LGNightPlayerPreKilledEvent;
import fr.leomelki.loupgarou.events.LGPlayerKilledEvent.Reason;
import fr.leomelki.loupgarou.events.LGPreDayStartEvent;
import fr.leomelki.loupgarou.events.LGVampiredEvent;

public class RGarde extends Role {
	private static final String IS_PROTECTED_BY_GUARD = "is_protected_by_guard";
	private static final String WAS_PROTECTED_BY_GUARD_LAST_NIGHT = "was_protected_by_guard_last_night";
	private static List<Reason> reasonsProtected = Arrays.asList(Reason.LOUP_GAROU, Reason.LOUP_BLANC,
			Reason.GM_LOUP_GAROU, Reason.ASSASSIN);

	public RGarde(LGGame game) {
		super(game);
	}

	@Override
	public RoleType getType() {
		return RoleType.VILLAGER;
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
		return "§a§lGarde";
	}

	@Override
	public String getFriendlyName() {
		return "du " + getName();
	}

	@Override
	public String getShortDescription() {
		return "Tu gagnes avec le §a§lVillage";
	}

	@Override
	public String getDescription() {
		return "Tu gagnes avec le §a§lVillage§f. Chaque nuit, tu peux te protéger toi ou quelqu'un d'autre des attaques §c§lhostiles§f. Tu ne peux pas protéger deux fois d’affilé la même personne.";
	}

	@Override
	public String getTask() {
		return "Choisis un joueur à protéger.";
	}

	@Override
	public String getBroadcastedTask() {
		return "Le " + getName() + "§9 choisit un joueur à protéger.";
	}

	@Override
	public int getTimeout() {
		return 15;
	}

	@Override
	protected void onNightTurn(LGPlayer player, Runnable callback) {
		player.showView();

		player.choose(new LGChooseCallback() {
			@Override
			public void callback(LGPlayer choosen) {
				if (choosen != null) {
					LGPlayer lastProtected = player.getCache().get(RGarde.WAS_PROTECTED_BY_GUARD_LAST_NIGHT);
					if (choosen == lastProtected) {
						if (lastProtected == player)
							player.sendMessage("§4§oTu t'es déjà protégé la nuit dernière.");
						else
							player.sendMessage(
									"§4§oTu as déjà protégé §7§l§o" + lastProtected.getFullName() + "§4§o la nuit dernière.");
					} else {
						if (choosen == player) {
							player.sendMessage("§6Tu décides de te protéger toi-même cette nuit.");
							player.sendActionBarMessage("§9Tu seras protégé.");
						} else {
							player.sendMessage("§6Tu vas protéger §7§l" + choosen.getFullName() + "§6 cette nuit.");
							player.sendActionBarMessage("§7§l" + choosen.getFullName() + "§9 sera protégé.");
						}
						choosen.getCache().set(RGarde.IS_PROTECTED_BY_GUARD, true);
						player.getCache().set(RGarde.WAS_PROTECTED_BY_GUARD_LAST_NIGHT, choosen);
						player.stopChoosing();
						player.hideView();
						callback.run();
					}
				}
			}
		});
	}

	@Override
	protected void onNightTurnTimeout(LGPlayer player) {
		player.getCache().remove(RGarde.WAS_PROTECTED_BY_GUARD_LAST_NIGHT);
		player.stopChoosing();
		player.hideView();
	}

	@EventHandler
	public void onPlayerKill(LGNightPlayerPreKilledEvent e) {
		if (e.getGame() == getGame() && reasonsProtected.contains(e.getReason())
				&& e.getKilled().getCache().has(RGarde.IS_PROTECTED_BY_GUARD)) {
			e.getKilled().getCache().remove(RGarde.IS_PROTECTED_BY_GUARD);
			e.setReason(Reason.DONT_DIE);
		}
	}

	@EventHandler
	public void onVampired(LGVampiredEvent e) {
		if (e.getGame() == getGame() && e.getPlayer().getCache().has(RGarde.IS_PROTECTED_BY_GUARD))
			e.setProtect(true);
	}

	@EventHandler
	public void onDayStart(LGPreDayStartEvent e) {
		if (e.getGame() == getGame())
			for (LGPlayer lgp : getGame().getInGame())
				lgp.getCache().remove(RGarde.IS_PROTECTED_BY_GUARD);
	}
}
