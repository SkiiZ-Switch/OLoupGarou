package fr.leomelki.loupgarou.roles;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import fr.leomelki.loupgarou.classes.LGGame;
import fr.leomelki.loupgarou.classes.LGPlayer;
import fr.leomelki.loupgarou.events.LGNightEndEvent;
import fr.leomelki.loupgarou.events.LGNightPlayerPreKilledEvent;
import fr.leomelki.loupgarou.events.LGPlayerKilledEvent.Reason;
import fr.leomelki.loupgarou.events.LGRoleTurnEndEvent;

public class RChaperonRouge extends Role {
	private static String immunityFromWolves = "immunity_from_wolves_chaperon";

	public RChaperonRouge(LGGame game) {
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

		return (amount > 1) ? baseline.replace("haperon", "haperons") : baseline;
	}

	@Override
	public String getName() {
		return "§a§lChaperon Rouge";
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
		return "Tu gagnes avec le §a§lVillage§f. Tant que le §a§lChasseur§f est en vie, tu ne peux pas te faire tuer par les §c§lLoups§f pendant la nuit.";
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

	@EventHandler(priority = EventPriority.LOWEST)
	public void onKill(LGNightPlayerPreKilledEvent e) {
		if (e.getKilled().getRole() == this && e.getReason() == Reason.LOUP_GAROU
				|| e.getReason() == Reason.GM_LOUP_GAROU && e.getKilled().isRoleActive()) {
			for (Role role : getGame().getRoles())
				if (role instanceof RChasseur && !role.getPlayers().isEmpty()) {
					e.getKilled().setProperty(RChaperonRouge.immunityFromWolves);
					e.setReason(Reason.DONT_DIE);
					break;
				}
		}
	}

	@EventHandler
	public void onTour(LGRoleTurnEndEvent e) {
		if (e.getGame() == getGame()) {
			if (e.getPreviousRole() instanceof RLoupGarou) {
				for (LGPlayer lgp : getGame().getAlive())
					if (lgp.hasProperty(RChaperonRouge.immunityFromWolves) && lgp.isRoleActive()) {
						for (LGPlayer l : getGame().getInGame())
							if (l.getRoleType() == RoleType.LOUP_GAROU)
								l.sendMessage(Role.IS_IMMUNE_FROM_WOLVES);
					}
			} else if (e.getPreviousRole() instanceof RGrandMechantLoup) {
				for (LGPlayer lgp : getGame().getAlive())
					if (lgp.hasProperty(RChaperonRouge.immunityFromWolves) && lgp.isRoleActive()) {
						for (LGPlayer l : e.getPreviousRole().getPlayers())
							l.sendMessage(Role.IS_IMMUNE_FROM_WOLVES);
					}
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onDayStart(LGNightEndEvent e) {
		if (e.getGame() == getGame()) {
			for (LGPlayer lgp : getPlayers())
				if (lgp.hasProperty(RChaperonRouge.immunityFromWolves)) {
					lgp.removeProperty(RChaperonRouge.immunityFromWolves);
					lgp.sendMessage("§9§oTu as été attaqué cette nuit.");
				}
		}
	}
}
