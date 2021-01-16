package fr.leomelki.loupgarou.roles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import fr.leomelki.loupgarou.MainLg;
import fr.leomelki.loupgarou.classes.LGCustomItems;
import fr.leomelki.loupgarou.classes.LGGame;
import fr.leomelki.loupgarou.classes.LGPlayer;
import lombok.Getter;
import lombok.Setter;

public abstract class Role implements Listener {
	@Getter @Setter private int waitedPlayers;
	@Getter private ArrayList<LGPlayer> players = new ArrayList<>(); 
	@Getter public List<LGPlayer> playersThisRound = new ArrayList<>(); 
	@Getter private final LGGame game;
	protected static final String PERFORMED_NO_ACTION = "§6Tu n'as rien fait cette nuit.";
	protected static final String IS_IMMUNE_FROM_WOLVES = "§cVotre cible est immunisée.";

	public Role(LGGame game) {
		this.game = game;

		Bukkit.getPluginManager().registerEvents(this, MainLg.getInstance());

		FileConfiguration config = MainLg.getInstance().getConfig();
		String roleConfigName = "distributionFixed." + getClass().getSimpleName().substring(1);

		if (config.contains(roleConfigName)) {
			waitedPlayers = config.getInt(roleConfigName);
		}
	}

	public abstract String getName(int amount);
	public abstract String getName();
	public abstract String getFriendlyName();
	public abstract String getShortDescription();
	public abstract String getDescription();
	public abstract String getTask();
	public abstract String getBroadcastedTask();

	public RoleType getType(LGPlayer lgp) {
		return getType();
	}

	public RoleWinType getWinType(LGPlayer lgp) {
		return getWinType();
	}

	public RoleType getType() {
		return null;
	}

	public RoleWinType getWinType() {
		return null;
	}

	/**
	 * @return Timeout in second for this role
	 */
	public abstract int getTimeout();

	public void onNightTurn(Runnable callback) {
		ArrayList<LGPlayer> playersCopy = new ArrayList<>(getPlayers());
		new Runnable() {

			@Override
			public void run() {
				getGame().cancelWait();
				if (playersCopy.isEmpty()) {
					onTurnFinish(callback);
					return;
				}
				LGPlayer player = playersCopy.remove(0);
				if (player.isRoleActive()) {
					getGame().wait(getTimeout(), () -> {
						try {
							Role.this.onNightTurnTimeout(player);
						} catch (Exception err) {
							System.out.println("Error when timeout role");
							err.printStackTrace();
						}
						this.run();
					}, (currentPlayer, secondsLeft) -> currentPlayer == player ? "§9§lC'est à ton tour !"
							: "§6C'est au tour " + getFriendlyName() + " §6(§e" + secondsLeft + " s§6)");
					player.sendMessage("§6" + getTask());
					onNightTurn(player, this);
				} else {
					getGame().wait(getTimeout(), () -> {
					}, (currentPlayer, secondsLeft) -> currentPlayer == player ? "§c§lTu ne peux pas jouer"
							: "§6C'est au tour " + getFriendlyName() + " §6(§e" + secondsLeft + " s§6)");
					Runnable run = this;
					new BukkitRunnable() {

						@Override
						public void run() {
							run.run();
						}
					}.runTaskLater(MainLg.getInstance(),
							(long) 20 * (ThreadLocalRandom.current().nextInt(getTimeout() / 3 * 2 - 4) + 4));
				}
			}
		}.run();
	}

	public void join(LGPlayer player, boolean sendMessage) {
		final String joinLog = player.getFullName() + " est " + getName();

		System.out.println(joinLog.replaceAll("\\§.", ""));
		
		this.players.add(player);
		this.playersThisRound.add(player);
		
		if (player.getRole() == null) {
			player.setRole(this);
		}

		waitedPlayers--;

		if (sendMessage) {
			player.sendTitle("§6Tu es " + getName(), "§e" + getShortDescription(), 200);
			player.sendMessage("§6Tu es " + getName() + "§6.");
			player.sendMessage("§6Description : §f" + getDescription());
		}
	}

	public void join(final LGPlayer player) {
		join(player, !getGame().isStarted());
		LGCustomItems.updateItem(player);
	}

	public void joinAndDisplayRole(final LGPlayer player) {
		join(player, true);
	}

	public void updateItemsForAllMembers() {
		for (LGPlayer player: this.players) {
			LGCustomItems.updateItem(player);
		}
	}

	public boolean hasPlayersLeft() {
		return getPlayers().size() > 0;
	}

	protected void onNightTurnTimeout(LGPlayer player) {
	}

	protected void onNightTurn(LGPlayer player, Runnable callback) {
	}

	protected void onTurnFinish(Runnable callback) {
		callback.run();
	}

	public int getTurnOrder() {
		try {
			RoleSort role = RoleSort.valueOf(getClass().getSimpleName().substring(1));
			return role == null ? -1 : role.ordinal();
		} catch (Exception e) {
			return -1;
		}
	}// En combientième ce rôle doit être appellé
}
