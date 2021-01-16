package fr.leomelki.loupgarou.roles;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import fr.leomelki.loupgarou.classes.LGGame;
import fr.leomelki.loupgarou.classes.LGPlayer;
import fr.leomelki.loupgarou.classes.LGPlayer.LGChooseCallback;
import fr.leomelki.loupgarou.classes.LGWinType;
import fr.leomelki.loupgarou.events.LGEndCheckEvent;
import fr.leomelki.loupgarou.events.LGGameEndEvent;
import fr.leomelki.loupgarou.events.LGNightEndEvent;
import fr.leomelki.loupgarou.events.LGNightPlayerPreKilledEvent;
import fr.leomelki.loupgarou.events.LGPlayerKilledEvent.Reason;
import fr.leomelki.loupgarou.events.LGPyromaneGasoilEvent;
import fr.leomelki.loupgarou.events.LGRoleTurnEndEvent;
import fr.leomelki.loupgarou.events.LGVampiredEvent;

public class RAssassin extends Role {
	private static final String IMMUNITY_FROM_WOLVES = "immunity_from_wolves_assassin";

	public RAssassin(LGGame game) {
		super(game);
	}

	@Override
	public RoleType getType() {
		return RoleType.NEUTRAL;
	}

	@Override
	public RoleWinType getWinType() {
		return RoleWinType.SEUL;
	}

	@Override
	public String getName(int amount) {
		final String baseline = this.getName();

		return (amount > 1) ? baseline + "s" : baseline;
	}

	@Override
	public String getName() {
		return "§1§lAssassin";
	}

	@Override
	public String getFriendlyName() {
		return "de l'" + getName();
	}

	@Override
	public String getShortDescription() {
		return "Tu gagnes §7§lSEUL";
	}

	@Override
	public String getDescription() {
		return "Tu gagnes §7§lSEUL§f. Chaque nuit, tu peux choisir un joueur à éliminer. Tu es immunisé contre l'attaque des §c§lLoups§f.";
	}

	@Override
	public String getTask() {
		return "Choisis un joueur à éliminer.";
	}

	@Override
	public String getBroadcastedTask() {
		return "L'" + getName() + "§9 ne controle plus ses pulsions...";
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
				if (choosen != null && choosen != player) {
					getGame().kill(choosen, Reason.ASSASSIN);
					player.sendActionBarMessage("§e§l" + choosen.getFullName() + "§6 va mourir");
					player.sendMessage("§6Tu as choisi de tuer §7§l" + choosen.getFullName() + "§6.");
					player.stopChoosing();
					player.hideView();
					callback.run();
				}
			}
		});
	}

	@EventHandler
	public void onKill(LGNightPlayerPreKilledEvent e) {
		// Les assassins ne peuvent pas mourir la nuit !
		if (e.getKilled().getRole() == this && e.getReason() == Reason.LOUP_GAROU
				|| e.getReason() == Reason.GM_LOUP_GAROU && e.getKilled().isRoleActive()) {
			e.setReason(Reason.DONT_DIE);
			e.getKilled().setProperty(RAssassin.IMMUNITY_FROM_WOLVES);
		}
	}

	@EventHandler
	public void onTour(LGRoleTurnEndEvent e) {
		if (e.getGame() == getGame()) {
			if (e.getPreviousRole() instanceof RLoupGarou) {
				for (LGPlayer lgp : getGame().getAlive())
					if (lgp.hasProperty(RAssassin.IMMUNITY_FROM_WOLVES)) {
						for (LGPlayer l : getGame().getInGame())
							if (l.getRoleType() == RoleType.LOUP_GAROU)
								l.sendMessage(Role.IS_IMMUNE_FROM_WOLVES);
					}
			} else if (e.getPreviousRole() instanceof RGrandMechantLoup) {
				for (LGPlayer lgp : getGame().getAlive())
					if (lgp.hasProperty(RAssassin.IMMUNITY_FROM_WOLVES)) {
						for (LGPlayer l : e.getPreviousRole().getPlayers())
							l.sendMessage(Role.IS_IMMUNE_FROM_WOLVES);
					}
			}
		}
	}

	@EventHandler
	public void onPyroGasoil(LGPyromaneGasoilEvent e) {
		if (e.getPlayer().getRole() == this && e.getPlayer().isRoleActive())
			e.setCancelled(true);
	}

	@EventHandler
	public void onVampired(LGVampiredEvent e) {
		if (e.getPlayer().getRole() == this && e.getPlayer().isRoleActive())
			e.setImmuned(true);
	}

	@EventHandler
	public void onDayStart(LGNightEndEvent e) {
		if (e.getGame() == getGame()) {
			for (LGPlayer lgp : getGame().getAlive()) {
				if (lgp.hasProperty(RAssassin.IMMUNITY_FROM_WOLVES)) {
					lgp.removeProperty(RAssassin.IMMUNITY_FROM_WOLVES);
				}
			}
		}
	}

	@EventHandler
	public void onEndgameCheck(LGEndCheckEvent e) {
		if (e.getGame() == getGame() && e.getWinType() == LGWinType.SOLO && !getPlayers().isEmpty()) {
			if (getPlayers().size() > 1)
				for (LGPlayer lgp : getPlayers())
					if (!lgp.isRoleActive())
						return;
			e.setWinType(LGWinType.ASSASSIN);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onEndGame(LGGameEndEvent e) {
		if (e.getWinType() == LGWinType.ASSASSIN) {
			e.getWinners().clear();
			e.getWinners().addAll(getPlayers());
		}
	}

	@Override
	protected void onNightTurnTimeout(LGPlayer player) {
		player.stopChoosing();
		player.hideView();
	}
}
