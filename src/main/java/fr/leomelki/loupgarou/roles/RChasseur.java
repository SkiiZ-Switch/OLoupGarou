package fr.leomelki.loupgarou.roles;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;

import fr.leomelki.loupgarou.classes.LGGame;
import fr.leomelki.loupgarou.classes.LGPlayer;
import fr.leomelki.loupgarou.events.LGDayStartEvent;
import fr.leomelki.loupgarou.events.LGGameEndEvent;
import fr.leomelki.loupgarou.events.LGNightStart;
import fr.leomelki.loupgarou.events.LGPlayerKilledEvent;
import fr.leomelki.loupgarou.events.LGPlayerKilledEvent.Reason;

public class RChasseur extends Role {
	ArrayList<LGPlayer> needToPlay = new ArrayList<>();

	public RChasseur(LGGame game) {
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
		return "§a§lChasseur";
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
		return "Tu gagnes avec le §a§lVillage§f. À ta mort, tu dois éliminer un joueur en utilisant ta dernière balle.";
	}

	@Override
	public String getTask() {
		return "Tu dois choisir qui va mourir avec toi.";
	}

	@Override
	public String getBroadcastedTask() {
		return "Le " + getName() + "§9 choisit qui il va emporter avec lui.";
	}

	@Override
	public int getTimeout() {
		return 15;
	}

	@Override
	protected void onNightTurn(LGPlayer player, Runnable callback) {
		getGame().wait(getTimeout(), () -> {
			this.onNightTurnTimeout(player);
			callback.run();
		}, (currentPlayer, secondsLeft) -> currentPlayer == player ? "§9§lC'est à ton tour !"
				: "§6Le Chasseur choisit sa cible (§e" + secondsLeft + " s§6)");
		getGame().broadcastMessage("§9" + getBroadcastedTask());
		player.sendMessage("§6" + getTask());
		player.choose(choosen -> {
			if (choosen != null) {
				player.stopChoosing();
				getGame().cancelWait();
				LGPlayerKilledEvent killEvent = new LGPlayerKilledEvent(getGame(), choosen, Reason.CHASSEUR);
				Bukkit.getPluginManager().callEvent(killEvent);
				if (killEvent.isCancelled())
					return;

				if (getGame().kill(killEvent.getKilled(), killEvent.getReason(), true))
					return;
				callback.run();
			}
		}, player);
	}

	@Override
	protected void onNightTurnTimeout(LGPlayer player) {
		getGame().broadcastMessage("§9Il n'a pas tiré sur la détente...");
		player.stopChoosing();
	}

	@EventHandler
	public void onPlayerKill(LGPlayerKilledEvent e) {
		if (e.getKilled().getRole() == this && e.getReason() != Reason.DISCONNECTED && e.getKilled().isRoleActive())
			needToPlay.add(e.getKilled());
	}

	@EventHandler
	public void onDayStart(LGDayStartEvent e) {
		if (e.getGame() != getGame())
			return;

		if (!needToPlay.isEmpty())
			e.setCancelled(true);

		if (!e.isCancelled())
			return;
		new Runnable() {
			public void run() {
				if (needToPlay.isEmpty()) {
					e.getGame().startDay();
					return;
				}
				LGPlayer player = needToPlay.remove(0);
				onNightTurn(player, this);
			}
		}.run();
	}

	@EventHandler
	public void onEndGame(LGGameEndEvent e) {
		if (e.getGame() != getGame())
			return;

		if (!needToPlay.isEmpty())
			e.setCancelled(true);

		if (!e.isCancelled())
			return;
		new Runnable() {
			public void run() {
				if (needToPlay.isEmpty()) {
					e.getGame().checkEndGame(true);
					return;
				}
				LGPlayer player = needToPlay.remove(0);
				onNightTurn(player, this);
			}
		}.run();
	}

	@EventHandler
	public void onNight(LGNightStart e) {
		if (e.getGame() == getGame() && !e.isCancelled() && !needToPlay.isEmpty()) {
			e.setCancelled(true);
			new Runnable() {
				public void run() {
					if (needToPlay.isEmpty()) {
						e.getGame().nextNight();
						return;
					}
					LGPlayer player = needToPlay.remove(0);
					onNightTurn(player, this);
				}
			}.run();
		}
	}
}
