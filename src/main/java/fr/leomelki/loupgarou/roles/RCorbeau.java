package fr.leomelki.loupgarou.roles;

import org.bukkit.event.EventHandler;
import org.bukkit.scheduler.BukkitRunnable;

import fr.leomelki.loupgarou.MainLg;
import fr.leomelki.loupgarou.classes.LGGame;
import fr.leomelki.loupgarou.classes.LGPlayer;
import fr.leomelki.loupgarou.classes.LGPlayer.LGChooseCallback;
import fr.leomelki.loupgarou.events.LGDayEndEvent;
import fr.leomelki.loupgarou.events.LGVoteEvent;

public class RCorbeau extends Role {
	private static final String CHOSEN_BY_RAVEN = "chosen_by_raven";

	public RCorbeau(LGGame game) {
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

		return (amount > 1) ? baseline + "x" : baseline;
	}

	@Override
	public String getName() {
		return "§a§lCorbeau";
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
		return "Tu gagnes avec le §a§lVillage§f. Chaque nuit, tu peux désigner un joueur qui se retrouvera le lendemain avec deux voix contre lui au vote.";
	}

	@Override
	public String getTask() {
		return "Tu peux choisir un joueur qui aura deux votes contre lui.";
	}

	@Override
	public String getBroadcastedTask() {
		return "Le " + getName() + "§9 s'apprête à diffamer quelqu'un...";
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
					choosen.setProperty(RCorbeau.CHOSEN_BY_RAVEN);
					player.sendActionBarMessage("§e§l" + choosen.getFullName() + "§6 aura deux votes contre lui");
					player.sendMessage("§6Tu nuis à la réputation de §7§l" + choosen.getFullName() + "§6.");
					player.stopChoosing();
					player.hideView();
					callback.run();
				}
			}
		});
	}

	@EventHandler
	public void onNightStart(LGDayEndEvent e) {
		if (e.getGame() == getGame())
			for (LGPlayer lgp : getGame().getAlive())
				lgp.removeProperty(RCorbeau.CHOSEN_BY_RAVEN);
	}

	@EventHandler
	public void onVoteStart(LGVoteEvent e) {
		if (e.getGame() == getGame())
			for (LGPlayer lgp : getGame().getAlive())
				if (lgp.hasProperty(RCorbeau.CHOSEN_BY_RAVEN)) {
					lgp.removeProperty(RCorbeau.CHOSEN_BY_RAVEN);
					LGPlayer lg = lgp;
					new BukkitRunnable() {

						@Override
						public void run() {
							getGame().getVote().vote(new LGPlayer("§a§lLe corbeau"), lg);
							getGame().getVote().vote(new LGPlayer("§a§lLe corbeau"), lg);// fix
							getGame().broadcastMessage("§7§l" + lg.getFullName() + "§6 a reçu la visite du " + getName() + "§6.");
						}
					}.runTask(MainLg.getInstance());

				}
	}

	@Override
	protected void onNightTurnTimeout(LGPlayer player) {
		player.stopChoosing();
		player.hideView();
	}
}
