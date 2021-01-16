package fr.leomelki.loupgarou.roles;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import fr.leomelki.loupgarou.classes.LGCustomItems;
import fr.leomelki.loupgarou.classes.LGGame;
import fr.leomelki.loupgarou.classes.LGPlayer;
import fr.leomelki.loupgarou.classes.LGVote;
import fr.leomelki.loupgarou.classes.LGWinType;
import fr.leomelki.loupgarou.classes.LGCustomItems.LGCustomItemsConstraints;
import fr.leomelki.loupgarou.classes.chat.LGChat;
import fr.leomelki.loupgarou.events.LGCustomItemChangeEvent;
import fr.leomelki.loupgarou.events.LGGameEndEvent;
import fr.leomelki.loupgarou.events.LGNightEndEvent;
import fr.leomelki.loupgarou.events.LGPlayerKilledEvent.Reason;
import fr.leomelki.loupgarou.events.LGUpdatePrefixEvent;
import fr.leomelki.loupgarou.events.LGVampiredEvent;
import lombok.Getter;

public class RVampire extends Role {
	public static final String INFECTED_BY_VAMPIRE = "infected_by_vampire";
	protected static final String INFECTED_BY_VAMPIRE_THIS_NIGHT = "infected_by_vampire_this_night";
	int nextCanInfect = 0;
	LGVote vote;

	public RVampire(LGGame game) {
		super(game);
	}

	@Override
	public String getName(int amount) {
		final String baseline = this.getName();

		return (amount > 1) ? baseline + "s" : baseline;
	}

	@Override
	public String getName() {
		return "§5§lVampire";
	}

	@Override
	public String getFriendlyName() {
		return "des §5§lVampires";
	}

	@Override
	public String getShortDescription() {
		return "Tu gagnes avec les §5§lVampires";
	}

	@Override
	public String getDescription() {
		return "Tu gagnes avec les §5§lVampires§f. Chaque nuit, tu te réunis avec tes compères pour décider d'une victime à transformer en §5§lVampire§f... Lorsqu'une transformation a lieu, tous les §5§lVampires§f doivent se reposer la nuit suivante. Un joueur transformé perd tous les pouvoirs liés à son ancien rôle, et gagne avec les §5§lVampires§f.";
	}

	@Override
	public String getTask() {
		return "Votez pour une cible à mordre.";
	}

	@Override
	public String getBroadcastedTask() {
		return "Les §5§lVampires§9 choisissent leur cible.";
	}

	@Override
	public RoleType getType() {
		return RoleType.VAMPIRE;
	}

	@Override
	public RoleWinType getWinType() {
		return RoleWinType.VAMPIRE;
	}

	@Override
	public int getTimeout() {
		return 30;
	}

	@Override
	public boolean hasPlayersLeft() {
		return nextCanInfect < getGame().getNight() && super.hasPlayersLeft();
	}

	@Getter
	private LGChat chat = new LGChat((sender, message) -> "§5" + sender.getFullName() + " §6» §f" + message);

	@Override
	public void join(LGPlayer player, boolean sendMessage) {
		super.join(player, sendMessage);
		for (LGPlayer p : getPlayers())
			p.updatePrefix();
	}

	@Override
	public void onNightTurn(Runnable callback) {
		vote = new LGVote(getTimeout(), getTimeout() / 3, getGame(), false, false, (player, secondsLeft) -> {
			if (!getPlayers().contains(player)) {
				return "§6C'est au tour " + getFriendlyName() + " §6(§e" + secondsLeft + " s§6)";
			}

			if (player.getCache().has("vote")) {
				return "§l§9Vous votez pour §c§l" + player.getCache().<LGPlayer>get("vote").getFullName();
			}

			return "§6Il vous reste §e" + secondsLeft + " seconde" + (secondsLeft > 1 ? "s" : "") + "§6 pour voter";
		});
		for (LGPlayer lgp : getGame().getAlive())
			if (lgp.getRoleType() == RoleType.VAMPIRE)
				lgp.showView();
		for (LGPlayer player : getPlayers()) {
			player.sendMessage("§6" + getTask());
			player.joinChat(chat);
		}
		vote.start(getPlayers(), getPlayers(), () -> {
			onNightTurnEnd();
			callback.run();
		}, getPlayers());
	}

	private void onNightTurnEnd() {
		for (LGPlayer lgp : getGame().getAlive())
			if (lgp.getRoleType() == RoleType.VAMPIRE)
				lgp.hideView();
		for (LGPlayer player : getPlayers())
			player.leaveChat();

		LGPlayer choosen = vote.getChoosen();
		if (choosen == null && !vote.getVotes().isEmpty()) {
			int max = 0;
			boolean equal = false;
			for (Entry<LGPlayer, List<LGPlayer>> entry : vote.getVotes().entrySet())
				if (entry.getValue().size() > max) {
					equal = false;
					max = entry.getValue().size();
					choosen = entry.getKey();
				} else if (entry.getValue().size() == max) {
					equal = true;
				}
			if (equal) {
				choosen = null;
				ArrayList<LGPlayer> choosable = new ArrayList<>();
				for (Entry<LGPlayer, List<LGPlayer>> entry : vote.getVotes().entrySet())
					if (entry.getValue().size() == max && entry.getKey().getRoleType() != RoleType.VAMPIRE)
						choosable.add(entry.getKey());
				if (!choosable.isEmpty())
					choosen = choosable.get(getGame().getRandom().nextInt(choosable.size()));
			}
		}
		if (choosen != null) {
			if (choosen.getRoleType() == RoleType.LOUP_GAROU || choosen.getRoleType() == RoleType.VAMPIRE) {
				for (LGPlayer player : getPlayers())
					player.sendMessage(Role.IS_IMMUNE_FROM_WOLVES);
				return;
			} else if (choosen.getRole() instanceof RChasseurDeVampire) {
				for (LGPlayer player : getPlayers())
					player.sendMessage(Role.IS_IMMUNE_FROM_WOLVES);
				getGame().kill(getPlayers().get(getPlayers().size() - 1), Reason.CHASSEUR_DE_VAMPIRE);
				return;
			}

			LGVampiredEvent event = new LGVampiredEvent(getGame(), choosen);
			Bukkit.getPluginManager().callEvent(event);
			if (event.isImmuned()) {
				for (LGPlayer player : getPlayers())
					player.sendMessage(Role.IS_IMMUNE_FROM_WOLVES);
				return;
			} else if (event.isProtect()) {
				for (LGPlayer player : getPlayers())
					player.sendMessage("§cVotre cible est protégée.");
				return;
			}
			for (LGPlayer player : getPlayers())
				player.sendMessage("§7§l" + choosen.getFullName() + " s'est transformé en §5§lVampire§6.");
			choosen.sendMessage("§6Tu as été infecté par les §5§lVampires §6pendant la nuit. Tu as perdu tes pouvoirs.");
			choosen.sendMessage("§6§oTu gagnes désormais avec les §5§l§oVampires§6§o.");
			choosen.getCache().set(RVampire.INFECTED_BY_VAMPIRE, true);
			choosen.getCache().set(RVampire.INFECTED_BY_VAMPIRE_THIS_NIGHT, true);
			nextCanInfect = getGame().getNight() + 1;
			join(choosen, false);
			LGCustomItems.updateItem(choosen);
		} else {
			for (LGPlayer player : getPlayers())
				player.sendMessage("§6Personne n'a été infecté.");
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onDayStart(LGNightEndEvent e) {
		if (e.getGame() == getGame())
			for (LGPlayer player : getGame().getAlive()) {
				if (player.getCache().getBoolean(RVampire.INFECTED_BY_VAMPIRE_THIS_NIGHT)) {
					player.getCache().remove(RVampire.INFECTED_BY_VAMPIRE_THIS_NIGHT);
					for (LGPlayer lgp : getGame().getInGame()) {
						if (lgp.getRoleType() == RoleType.VAMPIRE)
							lgp.sendMessage("§7§l" + player.getFullName() + "§6 s'est transformé en §5§lVampire§6...");
						else
							lgp.sendMessage("§6Quelqu'un s'est transformé en §5§lVampire§6...");
					}

					if (getGame().checkEndGame())
						e.setCancelled(true);
				}
			}
	}

	@EventHandler
	public void onGameEnd(LGGameEndEvent e) {
		if (e.getGame() == getGame() && e.getWinType() == LGWinType.VAMPIRE)
			for (LGPlayer lgp : getGame().getInGame())
				if (lgp.getRoleWinType() == RoleWinType.VAMPIRE)// Changed to wintype
					e.getWinners().add(lgp);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onUpdatePrefix(LGUpdatePrefixEvent e) {
		if (e.getGame() == getGame() && getPlayers().contains(e.getTo()) && getPlayers().contains(e.getPlayer()))
			e.setPrefix(e.getPrefix() + "§5");
	}

	@EventHandler
	public void onCustomItemChange(LGCustomItemChangeEvent e) {
		if (e.getGame() == getGame() && e.getPlayer().getCache().getBoolean(RVampire.INFECTED_BY_VAMPIRE))
			e.getConstraints().add(LGCustomItemsConstraints.VAMPIRE_INFECTE.getName());
	}
}
