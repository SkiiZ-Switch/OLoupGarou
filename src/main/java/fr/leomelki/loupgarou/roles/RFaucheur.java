package fr.leomelki.loupgarou.roles;

import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import fr.leomelki.loupgarou.MainLg;
import fr.leomelki.loupgarou.classes.LGGame;
import fr.leomelki.loupgarou.classes.LGPlayer;
import fr.leomelki.loupgarou.events.LGPlayerKilledEvent;
import fr.leomelki.loupgarou.events.LGPlayerKilledEvent.Reason;

public class RFaucheur extends Role {
	private static Random random = new Random();
	protected static final String EXTERMINATED_HIS_NEIGHBORS = "reaper_exterminated_his_neighbors";

	public RFaucheur(LGGame game) {
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
		return "§a§lFaucheur";
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
		return "Tu gagnes avec le §a§lVillage§f. Si les §c§lLoups-Garous§f te tuent pendant la nuit, tu emporteras l’un d’entre eux dans ta mort, mais si tu meurs lors du vote du §a§lvillage§f, ce sont tes deux voisins qui en paieront le prix.";
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
	public void onKill(LGPlayerKilledEvent e) {
		if (e.getKilled().getRole() == this && e.getKilled().isRoleActive()) {
			LGPlayer killed = e.getKilled();
			if (killed.getCache().has(RFaucheur.EXTERMINATED_HIS_NEIGHBORS))// A déjà fait son coup de faucheur !
				return;
			killed.getCache().set(RFaucheur.EXTERMINATED_HIS_NEIGHBORS, "true");
			if (e.getReason() == Reason.LOUP_GAROU || e.getReason() == Reason.GM_LOUP_GAROU) {// car le switch buggait (wtf)
				// Mort par les LG
				// Tue un lg au hasard
				LGPlayer selected = null;
				for (Role role : getGame().getRoles())
					if (role instanceof RLoupGarou)
						selected = role.getPlayers().get(random.nextInt(role.getPlayers().size()));
				if (selected != null) {
					LGPlayerKilledEvent killEvent = new LGPlayerKilledEvent(getGame(), e.getKilled(), e.getReason());
					Bukkit.getPluginManager().callEvent(killEvent);
					e.setKilled(selected);
					e.setReason(Reason.FAUCHEUR);
					if (killEvent.isCancelled())
						return;
					getGame().kill(killEvent.getKilled(), killEvent.getReason(), false);
				}
			} else if (e.getReason() == Reason.VOTE) {
				List<?> original = MainLg.getInstance().getConfig().getList("spawns");
				int size = original.size();
				int killedPlace = killed.getPlace();

				LGPlayer droite = null;
				LGPlayer gauche = null;
				for (int i = killedPlace + 1;; i++) {
					if (i == size)
						i = 0;
					LGPlayer lgp = getGame().getPlacements().get(i);
					if (lgp != null && !lgp.isDead()) {
						droite = lgp;
						break;
					}
					if (lgp == killed)// Fait un tour complet
						break;
				}
				for (int i = killedPlace - 1;; i--) {
					if (i == -1)
						i = size - 1;
					LGPlayer lgp = getGame().getPlacements().get(i);
					if (lgp != null && !lgp.isDead()) {
						gauche = lgp;
						break;
					}
					if (lgp == killed)// Fait un tour complet
						break;
				}
				if (droite != null) {
					LGPlayerKilledEvent killEvent = new LGPlayerKilledEvent(getGame(), e.getKilled(), e.getReason());
					Bukkit.getPluginManager().callEvent(killEvent);

					e.setKilled(droite);
					e.setReason(Reason.FAUCHEUR);

					if (!killEvent.isCancelled())
						getGame().kill(killEvent.getKilled(), killEvent.getReason(), false);
				}
				if (gauche != null) {
					LGPlayerKilledEvent killEvent;
					if (droite == null) {
						killEvent = new LGPlayerKilledEvent(getGame(), e.getKilled(), e.getReason());
						e.setKilled(gauche);
						e.setReason(Reason.FAUCHEUR);
					} else {
						killEvent = new LGPlayerKilledEvent(getGame(), gauche, Reason.FAUCHEUR);
					}
					Bukkit.getPluginManager().callEvent(killEvent);
					if (!killEvent.isCancelled())
						getGame().kill(killEvent.getKilled(), killEvent.getReason(), false);
				}
			}
		}
	}
}
