package fr.leomelki.loupgarou.classes;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.comphenix.protocol.wrappers.EnumWrappers.NativeGameMode;
import com.comphenix.protocol.wrappers.EnumWrappers.PlayerInfoAction;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;

import fr.leomelki.com.comphenix.packetwrapper.WrapperPlayServerChat;
import fr.leomelki.com.comphenix.packetwrapper.WrapperPlayServerEntityDestroy;
import fr.leomelki.com.comphenix.packetwrapper.WrapperPlayServerExperience;
import fr.leomelki.com.comphenix.packetwrapper.WrapperPlayServerPlayerInfo;
import fr.leomelki.com.comphenix.packetwrapper.WrapperPlayServerScoreboardObjective;
import fr.leomelki.com.comphenix.packetwrapper.WrapperPlayServerScoreboardTeam;
import fr.leomelki.com.comphenix.packetwrapper.WrapperPlayServerUpdateHealth;
import fr.leomelki.com.comphenix.packetwrapper.WrapperPlayServerUpdateTime;
import fr.leomelki.loupgarou.MainLg;
import fr.leomelki.loupgarou.classes.LGCustomItems.LGCustomItemsConstraints;
import fr.leomelki.loupgarou.classes.chat.LGChat;
import fr.leomelki.loupgarou.events.LGCustomItemChangeEvent;
import fr.leomelki.loupgarou.events.LGDayEndEvent;
import fr.leomelki.loupgarou.events.LGDayStartEvent;
import fr.leomelki.loupgarou.events.LGEndCheckEvent;
import fr.leomelki.loupgarou.events.LGGameEndEvent;
import fr.leomelki.loupgarou.events.LGGameJoinEvent;
import fr.leomelki.loupgarou.events.LGMayorVoteEvent;
import fr.leomelki.loupgarou.events.LGNightEndEvent;
import fr.leomelki.loupgarou.events.LGNightPlayerPreKilledEvent;
import fr.leomelki.loupgarou.events.LGNightStart;
import fr.leomelki.loupgarou.events.LGPlayerGotKilledEvent;
import fr.leomelki.loupgarou.events.LGPlayerKilledEvent;
import fr.leomelki.loupgarou.events.LGPlayerKilledEvent.Reason;
import fr.leomelki.loupgarou.events.LGPreDayStartEvent;
import fr.leomelki.loupgarou.events.LGRoleTurnEndEvent;
import fr.leomelki.loupgarou.events.LGSkinLoadEvent;
import fr.leomelki.loupgarou.events.LGVoteEvent;
import fr.leomelki.loupgarou.events.LGVoteLeaderChange;
import fr.leomelki.loupgarou.roles.RChienLoupLG;
import fr.leomelki.loupgarou.roles.REnfantSauvageLG;
import fr.leomelki.loupgarou.roles.Role;
import fr.leomelki.loupgarou.roles.RoleType;
import fr.leomelki.loupgarou.roles.RoleWinType;
import fr.leomelki.loupgarou.scoreboard.CustomScoreboard;
import fr.leomelki.loupgarou.utils.MultipleValueMap;
import fr.leomelki.loupgarou.utils.ItemBuilder;
import fr.leomelki.loupgarou.utils.VariousUtils;
import lombok.Getter;
import lombok.Setter;

public class LGGame implements Listener {
	private static boolean autoStart = false;

	@Getter private final SecureRandom random = new SecureRandom();
	@Getter private final int maxPlayers;
	@Getter private List<LGPlayer> inGame = new ArrayList<>();
	@Getter private List<Role> roles;

	@Getter private boolean started = false;
	@Getter private int night = 0;
	private BukkitTask startingTask;
	@Getter @Setter private int waitTicks;
	@Getter private boolean day;
	@Getter public long time = 0;
	@Getter private HashMap<Integer, LGPlayer> placements = new HashMap<>();

	@Getter private LGChat spectatorChat = new LGChat((sender, message) -> "(REKT) §7" + sender.getFullName() + " §6» §f" + message);
	@Getter private LGChat dayChat = new LGChat((sender, message) -> "§7" + sender.getFullName() + " §6» §f" + message);
	@Getter private MultipleValueMap<LGPlayerKilledEvent.Reason, LGPlayer> deaths = new MultipleValueMap<>();
	@Getter private LGPlayer mayor;

	private CustomScoreboard improvedScoreboard;
	private LGRoleDistributor roleDistributor;

	public LGGame(int maxPlayers) {
		this.maxPlayers = maxPlayers;
		Bukkit.getPluginManager().registerEvents(this, MainLg.getInstance());
	}

	public void sendActionBarMessage(String msg) {
		WrapperPlayServerChat chat = new WrapperPlayServerChat();
		chat.setPosition((byte) 2);
		chat.setMessage(WrappedChatComponent.fromText(msg));
		for (LGPlayer lgp : inGame)
			chat.sendPacket(lgp.getPlayer());
	}

	public void broadcastMessage(String msg) {
		for (LGPlayer lgp : inGame)
			lgp.sendMessage(msg);
	}

	public void broadcastSpacer() {
		for (LGPlayer lgp : inGame)
			lgp.getPlayer().sendMessage("\n");
	}

	private BukkitTask waitTask;

	public void wait(int seconds, Runnable callback) {
		wait(seconds, callback, null);
	}

	public void wait(int seconds, Runnable callback, TextGenerator generator) {
		cancelWait();
		waitTicks = seconds * 20;
		waitTask = new BukkitRunnable() {
			@Override
			public void run() {
				WrapperPlayServerExperience exp = new WrapperPlayServerExperience();
				exp.setLevel((short) (Math.floorDiv(waitTicks, 20) + 1));
				exp.setExperienceBar((float) waitTicks / (seconds * 20F));
				for (LGPlayer player : getInGame()) {
					exp.sendPacket(player.getPlayer());
					if (generator != null)
						player.sendActionBarMessage(generator.generate(player, Math.floorDiv(waitTicks, 20) + 1));
				}
				if (waitTicks == 0) {
					for (LGPlayer player : getInGame())
						player.sendActionBarMessage("");
					waitTask = null;
					cancel();
					callback.run();
				}
				waitTicks--;
			}
		}.runTaskTimer(MainLg.getInstance(), 0, 1);
	}

	public void wait(int seconds, int initialSeconds, Runnable callback, TextGenerator generator) {
		cancelWait();
		waitTicks = seconds * 20;
		waitTask = new BukkitRunnable() {
			@Override
			public void run() {
				WrapperPlayServerExperience exp = new WrapperPlayServerExperience();
				exp.setLevel((short) (Math.floorDiv(waitTicks, 20) + 1));
				exp.setExperienceBar((float) waitTicks / (initialSeconds * 20F));
				for (LGPlayer player : getInGame()) {
					exp.sendPacket(player.getPlayer());
					if (generator != null)
						player.sendActionBarMessage(generator.generate(player, Math.floorDiv(waitTicks, 20) + 1));
				}
				if (waitTicks == 0) {
					for (LGPlayer player : getInGame())
						player.sendActionBarMessage("");
					waitTask = null;
					cancel();
					callback.run();
				}
				waitTicks--;
			}
		}.runTaskTimer(MainLg.getInstance(), 0, 1);
	}

	public static interface TextGenerator {
		public String generate(LGPlayer player, int secondsLeft);
	}

	public void cancelWait() {
		if (waitTask != null) {
			waitTask.cancel();
			waitTask = null;
		}
	}

	public void kill(LGPlayer player, Reason reason) {
		if (!deaths.containsValue(player) && !player.isDead()) {
			LGNightPlayerPreKilledEvent event = new LGNightPlayerPreKilledEvent(this, player, reason);
			Bukkit.getPluginManager().callEvent(event);
			if (!event.isCancelled())
				deaths.put(event.getReason(), player);
		}
	}

	public boolean tryToJoin(LGPlayer lgp) {
		if (ended)
			return false;
		if (!started && inGame.size() < maxPlayers) {// Si la partie n'a pas démarrée et qu'il reste de la place
			lgp.getPlayer().removePotionEffect(PotionEffectType.INVISIBILITY);
			VariousUtils.setWarning(lgp.getPlayer(), false);
			if (lgp.isMuted())
				lgp.resetMuted();

			Player player = lgp.getPlayer();

			// Clear votes

			WrapperPlayServerEntityDestroy destroy = new WrapperPlayServerEntityDestroy();
			destroy.setEntityIds(new int[] { Integer.MIN_VALUE + player.getEntityId() });
			int[] ids = new int[getInGame().size() + 1];
			for (int i = 0; i < getInGame().size(); i++) {
				Player l = getInGame().get(i).getPlayer();
				if (l == null)
					continue;
				ids[i] = Integer.MIN_VALUE + l.getEntityId();
				destroy.sendPacket(l);
			}

			ids[ids.length - 1] = -player.getEntityId();// Clear voting

			destroy = new WrapperPlayServerEntityDestroy();
			destroy.setEntityIds(ids);
			destroy.sendPacket(player);

			// End clear votes/voting

			player.getInventory().clear();
			player.updateInventory();
			player.closeInventory();

			lgp.joinChat(dayChat);

			lgp.setGame(this);
			inGame.add(lgp);

			for (LGPlayer other : getInGame()) {
				other.updatePrefix();
				if (lgp != other) {
					player.hidePlayer(other.getPlayer());
					player.showPlayer(other.getPlayer());

					other.getPlayer().hidePlayer(player);
					other.getPlayer().showPlayer(player);
				}
			}

			lgp.getPlayer().setGameMode(GameMode.ADVENTURE);

			String fNick = MainLg.nicksFile.getString(lgp.getPlayer().getUniqueId().toString());
			if (fNick != null) {
				lgp.setNick(fNick);
				lgp.updatePrefix();
			}

			broadcastMessage("§7Le joueur §8" + lgp.getFullName() + "§7 a rejoint la partie §9(§8" + inGame.size() + "§7/§8"
					+ maxPlayers + "§9)");

			// Reset scoreboard
			WrapperPlayServerScoreboardObjective obj = new WrapperPlayServerScoreboardObjective();
			obj.setName("lg_scoreboard");
			obj.setMode(1);
			obj.sendPacket(player);

			Bukkit.getPluginManager().callEvent(new LGGameJoinEvent(this, lgp));
			// AutoStart
			if (autoStart)
				updateStart();
			return true;
		}
		return false;
	}

	public void checkLeave() {
		if (startingTask != null) {
			startingTask.cancel();
			startingTask = null;
			broadcastMessage("§c§oUn joueur s'est déconnecté. Le décompte de lancement a donc été arrêté.");
		}
	}

	public void updateStart() {
		if (!isStarted()) {

			if (inGame.size() == maxPlayers) {// Il faut que la partie soit totalement remplie pour qu'elle démarre car sinon,
																				// tous les rôles ne seraient pas distribués
				final MainLg mainLgInstance = MainLg.getInstance();
				final FileConfiguration config = mainLgInstance.getConfig();
				final boolean shouldShowScoreboard = config.getBoolean("showScoreboard");

				this.roleDistributor = new LGRoleDistributor(this, config, mainLgInstance.getRolesBuilder());
				this.improvedScoreboard = new CustomScoreboard(this.inGame, shouldShowScoreboard);
				this.improvedScoreboard.show();

				for (LGPlayer lgp : getInGame()) {
					final String meme = mainLgInstance.getRandomStartingMeme();
					if (meme != null) {
						lgp.sendMessage(meme);
					}
				}

				if (startingTask == null) {
					startingTask = new BukkitRunnable() {
						int timeLeft = 5 + 1;

						@Override
						public void run() {
							if (--timeLeft == 0)// start
								start();
							else
								sendActionBarMessage("§6Démarrage dans §e" + timeLeft + "§6...");
						}
					}.runTaskTimer(MainLg.getInstance(), 20, 20);
				}
			} else if (startingTask != null) {
				startingTask.cancel();
				broadcastMessage("§c§oLe démarrage de la partie a été annulé car une personne l'a quittée !");
			}
		}
	}

	public void start() {
		if (startingTask != null) {
			startingTask.cancel();
			startingTask = null;
		}
		MainLg.getInstance().loadConfig();
		started = true;

		// Registering roles
		List<?> original = MainLg.getInstance().getConfig().getList("spawns");
		List<Object> list = new ArrayList<>(original);
		for (LGPlayer lgp : getInGame()) {
			@SuppressWarnings("unchecked")
			List<Double> location = (List<Double>) list.remove(random.nextInt(list.size()));
			Player p = lgp.getPlayer();
			p.setWalkSpeed(0);
			p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 99999, 180, false, false));
			lgp.setPlace(original.indexOf(location));
			placements.put(lgp.getPlace(), lgp);
			p.teleport(new Location(p.getWorld(), location.get(0) + 0.5, location.get(1), location.get(2) + 0.5,
					location.get(3).floatValue(), location.get(4).floatValue()));
			WrapperPlayServerUpdateHealth update = new WrapperPlayServerUpdateHealth();
			update.setFood(6);
			update.setFoodSaturation(1);
			update.setHealth(20);
			update.sendPacket(p);
		}

		try {
			roles = this.roleDistributor.assignRoles();
		} catch (Exception err) {
			Bukkit.broadcastMessage("§4§lUne erreur est survenue lors de la création des roles... Regardez la console !");
			err.printStackTrace();
		}

		new BukkitRunnable() {
			int timeLeft = 5 * 2;
			int actualRole = getRoles().size();

			@Override
			public void run() {
				if (--timeLeft == 0) {
					cancel();
					_start();
					return;
				}

				if (timeLeft == 5 * 2 - 1) {
					for (LGPlayer lgp : getInGame()) {
						lgp.getPlayer().getInventory().clear();
						lgp.getPlayer().updateInventory();
					}
					broadcastMessage("§2Attribution des rôles...");
				}

				if (--actualRole < 0)
					actualRole = getRoles().size() - 1;

				ItemStack stack = new ItemStack(LGCustomItems.getItem(getRoles().get(actualRole)));
				for (LGPlayer lgp : getInGame()) {
					lgp.getPlayer().getInventory().setItemInOffHand(stack);
					lgp.getPlayer().updateInventory();
				}
			}
		}.runTaskTimer(MainLg.getInstance(), 0, 4);
	}

	private void _start() {
		broadcastMessage("§8§oDébut de la partie...");

		for (Role role: this.roles) {
			role.updateItemsForAllMembers();
		}

		started = true;
		updateRoleScoreboard();

		// Classe les roles afin de les appeler dans le bon ordre
		roles.sort(new Comparator<Role>() {
			@Override
			public int compare(Role role1, Role role2) {
				return role1.getTurnOrder() - role2.getTurnOrder();
			}
		});

		// Start day one
		nextNight(10);
	}

	public void updateRoleScoreboard() {
		final HashMap<Role, RolePlayers> roleMapping = new HashMap<>();

		for (LGPlayer lgp : getAlive()) {
			final Role playerRole = lgp.getRole();
			final RolePlayers currentIndex = roleMapping.get(playerRole);

			if (currentIndex == null) {
				roleMapping.put(playerRole, new RolePlayers(playerRole));
			} else {
				currentIndex.increment();
			}
		}

		final ArrayList<RolePlayers> activeRoles = new ArrayList<>(roleMapping.values());
		activeRoles.sort((a, b) -> {
			// TODO fix dégueu juste ici pour le chien loup lg à changer (2x)
			return (
				b.getAmountOfPlayers() + (
					b.getRole().getType() != RoleType.LOUP_GAROU ||
					b.getRole() instanceof RChienLoupLG ||
					b.getRole() instanceof REnfantSauvageLG ? (b.getRole().getType() == RoleType.NEUTRAL ? 0 : 999) : 200
				) -
				a.getAmountOfPlayers() - (
					a.getRole().getType() != RoleType.LOUP_GAROU ||
					a.getRole() instanceof RChienLoupLG ||
					a.getRole() instanceof REnfantSauvageLG ? (a.getRole().getType() == RoleType.NEUTRAL ? 0 : 999) : 200
				)
			);
		});

		this.improvedScoreboard.displayEntries(activeRoles);
	}

	public List<LGPlayer> getAlive() {
		return this.inGame.stream().filter(lgp -> !lgp.isDead()).collect(Collectors.toList());
	}

	public void nextNight() {
		nextNight(5);
	}

	public void nextNight(int timeout) {
		if (ended)
			return;
		LGNightStart event = new LGNightStart(this);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled())
			return;

		if (mayorKilled()) {// mort du maire
			broadcastMessage("§9Le §5§lCapitaine§9 est mort, il désigne un joueur en remplaçant.");
			getMayor().sendMessage("§6Choisis un joueur qui deviendra §5§lCapitaine§6 à son tour.");
			LGGame.this.wait(30, () -> {
				mayor.stopChoosing();
				setMayor(getAlive().get(random.nextInt(getAlive().size())));
				broadcastMessage("§7§l" + mayor.getFullName() + "§9 devient le nouveau §5§lCapitaine§9.");
				nextNight();
			}, (player, secondsLeft) -> "§e" + mayor.getFullName() + "§6 choisit qui sera le nouveau §5§lCapitaine§6 (§e"
					+ secondsLeft + " s§6)");

			mayor.choose(choosen -> {
				if (choosen != null) {
					mayor.stopChoosing();
					cancelWait();
					setMayor(choosen);
					broadcastMessage("§7§l" + mayor.getFullName() + "§9 devient le nouveau §5§lCapitaine§9.");
					nextNight();
				}
			}, mayor);
			return;
		}

		new BukkitRunnable() {
			int timeoutLeft = timeout * 20;

			@Override
			public void run() {
				if (--timeoutLeft <= 20 + 20 * 2) {
					if (timeoutLeft == 20)
						cancel();

					LGGame.this.time = (long) (18000 - (timeoutLeft - 20D) / (20 * 2D) * 12000D);
					WrapperPlayServerUpdateTime serverUpdateTime = new WrapperPlayServerUpdateTime();
					serverUpdateTime.setAgeOfTheWorld(0);
					serverUpdateTime.setTimeOfDay(LGGame.this.time);
					for (LGPlayer lgp : getInGame())
						serverUpdateTime.sendPacket(lgp.getPlayer());
				}
			}
		}.runTaskTimer(MainLg.getInstance(), 1, 1);
		LGGame.this.wait(timeout, this::nextNight_, (player, secondsLeft) -> "§6La nuit va tomber dans §e" + secondsLeft
				+ " seconde" + (secondsLeft > 1 ? "s" : ""));
	}

	private void nextNight_() {
		if (ended)
			return;
		night++;
		broadcastSpacer();
		broadcastMessage("§9----------- §lNuit n°" + night + "§9 -----------");
		broadcastMessage("§8§oLa nuit tombe sur le village...");
		for (LGPlayer player : getAlive())
			player.leaveChat();
		for (LGPlayer player : getInGame()) {
			player.stopAudio(LGSound.AMBIANT_DAY);
			player.playAudio(LGSound.START_NIGHT, 0.5);
			player.playAudio(LGSound.AMBIANT_NIGHT, 0.07);
		}
		day = false;
		Bukkit.getPluginManager().callEvent(new LGDayEndEvent(this));
		for (LGPlayer player : getInGame())
			player.hideView();

		ArrayList<Role> rolesCopy = new ArrayList<>(roles);
		new Runnable() {
			Role lastRole;

			public void run() {
				Runnable run = this;
				new BukkitRunnable() {

					@Override
					public void run() {
						if (rolesCopy.isEmpty()) {
							Bukkit.getPluginManager().callEvent(new LGRoleTurnEndEvent(LGGame.this, null, lastRole));
							lastRole = null;
							endNight();
							return;
						}
						Role role = rolesCopy.remove(0);
						Bukkit.getPluginManager().callEvent(new LGRoleTurnEndEvent(LGGame.this, role, lastRole));
						lastRole = role;
						if (role.getTurnOrder() == -1 || !role.hasPlayersLeft())
							this.run();
						else {
							broadcastMessage("§9" + role.getBroadcastedTask());
							role.onNightTurn(run);
						}
					}
				}.runTaskLater(MainLg.getInstance(), 60);
			}
		}.run();
	}

	public boolean kill(LGPlayer killed, Reason reason, boolean endGame) {
		if (killed.getPlayer() != null) {
			killed.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 999999, 1, false, false));
			killed.die();

			for (LGPlayer lgp : getInGame())
				if (lgp == killed) {
					WrapperPlayServerPlayerInfo info = new WrapperPlayServerPlayerInfo();
					ArrayList<PlayerInfoData> infos = new ArrayList<>();
					info.setAction(PlayerInfoAction.REMOVE_PLAYER);
					infos.add(new PlayerInfoData(new WrappedGameProfile(lgp.getPlayer().getUniqueId(), lgp.getName(true)), 0,
							NativeGameMode.ADVENTURE, WrappedChatComponent.fromText(lgp.getName(true))));
					info.setData(infos);
					info.sendPacket(lgp.getPlayer());
				} else {
					lgp.getPlayer().hidePlayer(killed.getPlayer());
				}

			if (vote != null)
				vote.remove(killed);

			final String deathLog = String.format(reason.getMessage(), killed.getFullName()) + ", il était "
					+ killed.getRole().getName() + (killed.isInfected() ? " §c§l(Infecté)" : "")
					+ (killed.isVampire() ? " §5§l(Vampire)" : "") + "§4.";

			broadcastMessage(deathLog);
			System.out.println(deathLog.replaceAll("\\§.", ""));

			// Lightning effect
			killed.getPlayer().getWorld().strikeLightningEffect(killed.getPlayer().getLocation());

			for (Role role : getRoles())
				if (role.getPlayers().contains(killed))
					role.getPlayers().remove(killed);

			killed.setDead(true);

			Bukkit.getPluginManager()
					.callEvent(new LGPlayerGotKilledEvent(this, killed, reason, !checkEndGame(false) && endGame));

			VariousUtils.setWarning(killed.getPlayer(), true);

			killed.getPlayer().getInventory().setHelmet(new ItemStack(Material.CARVED_PUMPKIN));

			LGCustomItems.updateItem(killed);

			killed.joinChat(spectatorChat);
			killed.joinChat(dayChat, true);
		}

		// Update scoreboard

		updateRoleScoreboard();

		// End update scoreboard

		if (!checkEndGame(false))
			return false;
		if (endGame)
			checkEndGame();
		return true;
	}

	private void showcaseVillage(List<LGPlayer> winners, LGWinType winType) {
		final boolean shouldDisplayWinners = (!winners.isEmpty());

		broadcastSpacer();
		broadcastMessage("§9----------- §lFIN DE LA PARTIE -----------");
		broadcastMessage(winType.getMessage());

		if (shouldDisplayWinners) {
			final List<String> winnerNames = winners.stream().map(LGPlayer::getFullName).collect(Collectors.toList());
			final String winnersFriendlyName = (winners.size() > 1) ? "aux vainqueurs" : "au vainqueur";
			broadcastMessage("§6§l§oFélicitations " + winnersFriendlyName + ": §7§l" + String.join(", §7", winnerNames));
		}

		broadcastSpacer();
		broadcastMessage("§e§lLa composition du village à cette partie était la suivante");

		// We unregister every role listener because they are unused after the game's
		// end !
		if (this.roles != null) {
			for (Role role : this.roles) {
				final List<String> playerNames = role.getPlayersThisRound().stream().map(LGPlayer::getFullName)
						.collect(Collectors.toList());
				broadcastMessage("§e - " + role.getName() + " §e: §7§l" + String.join(", §7", playerNames));
				HandlerList.unregisterAll(role);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onGameEnd(LGGameEndEvent e) {
		if (e.getGame() == this && e.getWinType() == LGWinType.VILLAGEOIS)
			for (LGPlayer lgp : getInGame())
				if (lgp.getRoleType() == RoleType.VILLAGER)
					e.getWinners().add(lgp);
	}

	@Setter
	boolean ended;

	public void endGame(LGWinType winType) {
		if (ended)
			return;

		ArrayList<LGPlayer> winners = new ArrayList<>();
		LGGameEndEvent event = new LGGameEndEvent(this, winType, winners);
		Bukkit.getPluginManager().callEvent(event);

		if (event.isCancelled())
			return;

		for (LGPlayer lgp : getInGame())// Avoid bugs
			if (lgp.getPlayer() != null)
				lgp.getPlayer().closeInventory();

		cancelWait();// Also avoid bugs
		ended = true;

		this.showcaseVillage(winners, winType);

		broadcastSpacer();

		for (LGPlayer lgp : getInGame()) {
			lgp.leaveChat();
			lgp.joinChat(spectatorChat);

			lgp.sendTitle("§7§lÉgalité", "§8Personne n'a gagné...", 200);

			if (winners.contains(lgp))
				lgp.sendTitle("§a§lVictoire !", "§6Vous avez gagné la partie.", 200);
			else if (winType == LGWinType.EQUAL || winType == LGWinType.NONE)
				lgp.sendTitle("§7§lÉgalité", "§8Personne n'a gagné...", 200);
			else
				lgp.sendTitle("§c§lDéfaite...", "§4Vous avez perdu la partie.", 200);

			Player p = lgp.getPlayer();
			lgp.showView();
			p.removePotionEffect(PotionEffectType.JUMP);
			p.setWalkSpeed(0.2f);
		}

		if (this.improvedScoreboard != null) {
			this.improvedScoreboard.hide();
		}

		for (LGPlayer lgp : getInGame())
			if (lgp.getPlayer().isOnline()) {
				LGPlayer.removePlayer(lgp.getPlayer());
				WrapperPlayServerScoreboardTeam team = new WrapperPlayServerScoreboardTeam();
				team.setMode(1);
				team.setName("you_are");
				team.sendPacket(lgp.getPlayer());
				LGPlayer.thePlayer(lgp.getPlayer()).join(MainLg.getInstance().getCurrentGame());
				if (lgp.getPlayer().hasPermission("loupgarou.admin")) {
					lgp.getPlayer().getInventory().setItem(1,
							new ItemBuilder(Material.CLAY).setName("Choisir les rôles").build());
					lgp.getPlayer().getInventory().setItem(3,
							new ItemBuilder(Material.EMERALD).setName("Lancer la partie").build());
				}
			}
	}

	public boolean mayorKilled() {
		return getMayor() != null && getMayor().isDead();
	}

	public void endNight() {
		if (ended)
			return;
		broadcastSpacer();
		broadcastMessage("§9----------- §lJour n°" + night + "§9 -----------");
		broadcastMessage("§8§oLe jour se lève sur le village...");

		for (LGPlayer p : getInGame()) {
			p.stopAudio(LGSound.AMBIANT_NIGHT);
			p.playAudio(LGSound.START_DAY, 0.5);
			p.playAudio(LGSound.AMBIANT_DAY, 0.07);
		}

		LGNightEndEvent eventNightEnd = new LGNightEndEvent(this);
		Bukkit.getPluginManager().callEvent(eventNightEnd);
		if (eventNightEnd.isCancelled())
			return;

		int died = 0;
		boolean endGame = false;

		for (Entry<Reason, LGPlayer> entry : deaths.entrySet()) {
			if (entry.getKey() == Reason.DONT_DIE)
				continue;
			if (entry.getValue().isDead())// On ne fait pas mourir quelqu'un qui est déjà mort (résout le problème du
																		// dictateur tué par le chasseur)
				continue;
			if (entry.getValue().getPlayer() != null) {// S'il a deco bah au moins ça crash pas hehe
				LGPlayerKilledEvent event = new LGPlayerKilledEvent(this, entry.getValue(), entry.getKey());
				Bukkit.getPluginManager().callEvent(event);
				if (!event.isCancelled()) {
					endGame |= kill(event.getKilled(), event.getReason(), false);
					died++;
				}
			}
		}
		deaths.clear();
		if (died == 0)
			broadcastMessage("§9Étonnamment, personne n'est mort cette nuit.");

		day = true;
		for (LGPlayer player : getInGame())
			player.showView();

		new BukkitRunnable() {
			int timeoutLeft = 20;

			@Override
			public void run() {
				if (timeoutLeft++ > 20) {
					if (timeoutLeft == 20 + (2 * 20))
						cancel();

					LGGame.this.time = (long) (18000 - (timeoutLeft - 20D) / (20 * 2D) * 12000D);
					WrapperPlayServerUpdateTime serverUpdateTime = new WrapperPlayServerUpdateTime();
					serverUpdateTime.setAgeOfTheWorld(0);
					serverUpdateTime.setTimeOfDay(LGGame.this.time);
					for (LGPlayer lgp : getInGame())
						serverUpdateTime.sendPacket(lgp.getPlayer());
				}
			}
		}.runTaskTimer(MainLg.getInstance(), 1, 1);

		LGPreDayStartEvent dayStart = new LGPreDayStartEvent(this);
		Bukkit.getPluginManager().callEvent(dayStart);
		if (!dayStart.isCancelled()) {
			if (endGame)
				checkEndGame();
			else
				startDay();
		}
	}

	public void startDay() {
		for (LGPlayer player : getInGame())
			player.joinChat(dayChat, player.isDead());

		LGDayStartEvent dayStart = new LGDayStartEvent(this);
		Bukkit.getPluginManager().callEvent(dayStart);
		if (dayStart.isCancelled())
			return;
		if (mayorKilled()) {// mort du maire
			broadcastMessage("§9Le §5§lCapitaine§9 est mort, il désigne un joueur en remplaçant.");
			getMayor().sendMessage("§6Choisis un joueur qui deviendra §5§lCapitaine§6 à son tour.");
			LGGame.this.wait(30, () -> {
				mayor.stopChoosing();
				setMayor(getAlive().get(random.nextInt(getAlive().size())));
				broadcastMessage("§7§l" + mayor.getFullName() + "§9 devient le nouveau §5§lCapitaine§9.");
				startDay();
			}, (player, secondsLeft) -> "§e" + mayor.getFullName() + "§6 choisit qui sera le nouveau §5§lCapitaine§6 (§e"
					+ secondsLeft + " s§6)");

			mayor.choose(choosen -> {
				if (choosen != null) {
					mayor.stopChoosing();
					cancelWait();
					setMayor(choosen);
					broadcastMessage("§7§l" + mayor.getFullName() + "§9 devient le nouveau §5§lCapitaine§9.");
					startDay();
				}
			}, mayor);
			return;
		}
		new BukkitRunnable() {

			@Override
			public void run() {
				if (getMayor() == null && getAlive().size() > 2)
					mayorVote();
				else
					peopleVote();
			}
		}.runTaskLater(MainLg.getInstance(), 40);
	}

	public void setMayor(LGPlayer mayor) {
		LGPlayer latestMayor = this.mayor;
		this.mayor = mayor;
		if (mayor != null && mayor.getPlayer().isOnline()) {
			LGCustomItems.updateItem(mayor);
			mayor.updateSkin();
			mayor.updateOwnSkin();
		}
		if (latestMayor != null && latestMayor.getPlayer() != null && latestMayor.getPlayer().isOnline()) {
			LGCustomItems.updateItem(latestMayor);
			latestMayor.updateSkin();
			latestMayor.updateOwnSkin();
		}
	}

	@EventHandler
	public void onCustomItemChange(LGCustomItemChangeEvent e) {
		if (e.getGame() == this) {
			if (getMayor() == e.getPlayer())
				e.getConstraints().add(LGCustomItemsConstraints.MAYOR.getName());
			if (e.getPlayer().isDead())
				e.getConstraints().add(LGCustomItemsConstraints.DEAD.getName());
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onSkinChange(LGSkinLoadEvent e) {
		if (e.getGame() == this) {
			e.getProfile().getProperties().removeAll("textures");
			if (getMayor() == e.getPlayer())
				e.getProfile().getProperties().put("textures", LGCustomSkin.MAYOR.getProperty());
			else
				e.getProfile().getProperties().put("textures", LGCustomSkin.VILLAGER.getProperty());
		}
	}

	private void mayorVote() {
		if (ended)
			return;
		LGMayorVoteEvent event = new LGMayorVoteEvent(this);
		Bukkit.getPluginManager().callEvent(event);
		if (!event.isCancelled()) {
			broadcastMessage("§9Il est temps de voter pour élire un §5§lCapitaine§9.");
			vote = new LGVote(180, 20, this, true, true,
					(player, secondsLeft) -> player.getCache().has("vote")
							? "§6Tu votes pour §7§l" + player.getCache().<LGPlayer>get("vote").getFullName()
							: "§6Il te reste §e" + secondsLeft + " seconde" + (secondsLeft > 1 ? "s" : "") + "§6 pour voter");
			vote.start(getAlive(), getInGame(), () -> {
				if (vote.getChoosen() == null)
					setMayor(getAlive().get(random.nextInt(getAlive().size())));
				else
					setMayor(vote.getChoosen());

				broadcastMessage("§7§l" + mayor.getFullName() + "§6 devient le §5§lCapitaine §6du village.");
				peopleVote();
			});
		}
	}

	@Getter
	private LGVote vote;
	boolean isPeopleVote = false;

	@EventHandler
	public void leaderChange(LGVoteLeaderChange e) {
		if (isPeopleVote && vote != null && e.getGame() == this) {
			for (LGPlayer player : e.getLatest())
				if (!e.getNow().contains(player))
					VariousUtils.setWarning(player.getPlayer(), false);

			for (LGPlayer player : e.getNow())
				if (!e.getLatest().contains(player))
					VariousUtils.setWarning(player.getPlayer(), true);
		}
	}

	private void peopleVote() {
		if (ended)
			return;
		LGVoteEvent event = new LGVoteEvent(this);
		Bukkit.getPluginManager().callEvent(event);
		if (!event.isCancelled()) {
			broadcastMessage("§9La phase des votes a commencé.");
			isPeopleVote = true;
			vote = new LGVote(180, 20, this, false, false,
					(player, secondsLeft) -> player.getCache().has("vote")
							? "§6Tu votes pour §7§l" + player.getCache().<LGPlayer>get("vote").getName()
							: "§6Il te reste §e" + secondsLeft + " seconde" + (secondsLeft > 1 ? "s" : "") + "§6 pour voter");
			vote.start(getAlive(), getInGame(), () -> {
				isPeopleVote = false;
				if (vote.getChoosen() == null || (vote.isMayorVote() && getMayor() == null))
					broadcastMessage(
							/* getMayor() != null ? "§9Le maire a décidé de gracier les accusés." : */"§9Personne n'est mort aujourd'hui.");
				else {
					LGPlayerKilledEvent killEvent = new LGPlayerKilledEvent(this, vote.getChoosen(), Reason.VOTE);
					Bukkit.getPluginManager().callEvent(killEvent);
					if (killEvent.isCancelled())// chassou ?
						return;
					if (kill(killEvent.getKilled(), killEvent.getReason(), true))
						return;
				}
				nextNight();
			}, mayor);
		} // Sinon c'est à celui qui a cancel de s'en occuper
	}

	public boolean checkEndGame() {
		return checkEndGame(true);
	}

	public boolean checkEndGame(boolean doEndGame) {
		int goodGuy = 0;
		int badGuy = 0;
		int solo = 0;
		int vampires = 0;
		for (LGPlayer lgp : getAlive())
			if (lgp.getRoleWinType() == RoleWinType.LOUP_GAROU)
				badGuy++;
			else if (lgp.getRoleWinType() == RoleWinType.VILLAGE)
				goodGuy++;
			else if (lgp.getRoleWinType() == RoleWinType.SEUL)
				solo++;
			else if (lgp.getRoleWinType() == RoleWinType.VAMPIRE)
				vampires++;
		LGEndCheckEvent event = new LGEndCheckEvent(this,
				goodGuy == 0 || badGuy == 0
						? (goodGuy + badGuy == 0 ? LGWinType.EQUAL : (goodGuy > 0 ? LGWinType.VILLAGEOIS : LGWinType.LOUPGAROU))
						: LGWinType.NONE);

		if ((badGuy + goodGuy > 0 && solo > 0) || solo > 1 || (badGuy + goodGuy > 0 && vampires > 0)
				|| (solo > 0 && vampires > 0))
			event.setWinType(LGWinType.NONE);

		if (badGuy + goodGuy == 0 && solo == 1 && vampires == 0)
			event.setWinType(LGWinType.SOLO);

		if (badGuy + goodGuy == 0 && solo == 0 && vampires > 0)
			event.setWinType(LGWinType.VAMPIRE);

		Bukkit.getPluginManager().callEvent(event);
		if (doEndGame && event.getWinType() != LGWinType.NONE)
			endGame(event.getWinType());
		return event.getWinType() != LGWinType.NONE;
	}
}
