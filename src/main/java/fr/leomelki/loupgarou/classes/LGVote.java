package fr.leomelki.loupgarou.classes;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.StringJoiner;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_15_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftPlayer;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import com.comphenix.protocol.wrappers.EnumWrappers.ItemSlot;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedDataWatcher.WrappedDataWatcherObject;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;

import fr.leomelki.com.comphenix.packetwrapper.WrapperPlayServerEntityDestroy;
import fr.leomelki.com.comphenix.packetwrapper.WrapperPlayServerEntityEquipment;
import fr.leomelki.com.comphenix.packetwrapper.WrapperPlayServerEntityLook;
import fr.leomelki.com.comphenix.packetwrapper.WrapperPlayServerEntityMetadata;
import fr.leomelki.com.comphenix.packetwrapper.WrapperPlayServerSpawnEntityLiving;
import fr.leomelki.loupgarou.MainLg;
import fr.leomelki.loupgarou.classes.LGGame.TextGenerator;
import fr.leomelki.loupgarou.classes.LGPlayer.LGChooseCallback;
import fr.leomelki.loupgarou.events.LGVoteLeaderChange;
import fr.leomelki.loupgarou.utils.VariousUtils;
import lombok.Getter;
import net.minecraft.server.v1_15_R1.DataWatcher;
import net.minecraft.server.v1_15_R1.DataWatcherObject;
import net.minecraft.server.v1_15_R1.Entity;
import net.minecraft.server.v1_15_R1.EntityArmorStand;
import net.minecraft.server.v1_15_R1.IChatBaseComponent;
import net.minecraft.server.v1_15_R1.PacketPlayOutEntityMetadata;

public class LGVote {
	@Getter LGPlayer choosen;
	private int timeout;
	private int initialTimeout;
	private int littleTimeout;
	private Runnable callback;
	private final LGGame game;
	@Getter private List<LGPlayer> participants;
	@Getter private List<LGPlayer> viewers;
	private final TextGenerator generator;
	@Getter private final HashMap<LGPlayer, List<LGPlayer>> votes = new HashMap<>();
	private int votesSize = 0;
	private LGPlayer mayor;
	private List<LGPlayer> latestTop = new ArrayList<>();
	private List<LGPlayer> blacklisted = new ArrayList<>();
	private final boolean randomIfEqual;
	@Getter private boolean mayorVote;
	private boolean ended;

	public LGVote(int timeout, int littleTimeout, LGGame game, boolean positiveVote, boolean randomIfEqual, TextGenerator generator) {
		this.littleTimeout = littleTimeout;
		this.initialTimeout = timeout;
		this.timeout = timeout;
		this.game = game;
		this.generator = generator;
		this.randomIfEqual = randomIfEqual;
	}

	public void start(List<LGPlayer> participants, List<LGPlayer> viewers, Runnable callback) {
		this.callback = callback;
		this.participants = participants;
		this.viewers = viewers;
		game.wait(timeout, this::end, generator);
		for (LGPlayer player : participants)
			player.choose(getChooseCallback(player));
	}

	public void start(List<LGPlayer> participants, List<LGPlayer> viewers, Runnable callback,
			List<LGPlayer> blacklisted) {
		this.callback = callback;
		this.participants = participants;
		this.viewers = viewers;
		game.wait(timeout, this::end, generator);
		for (LGPlayer player : participants)
			player.choose(getChooseCallback(player));
		this.blacklisted = blacklisted;
	}

	public void start(List<LGPlayer> participants, List<LGPlayer> viewers, Runnable callback, LGPlayer mayor) {
		this.callback = callback;
		this.participants = participants;
		this.viewers = viewers;
		this.mayor = mayor;
		game.wait(timeout, this::end, generator);
		for (LGPlayer player : participants)
			player.choose(getChooseCallback(player));
	}

	private static DataWatcherObject<Optional<IChatBaseComponent>> az;
	private static DataWatcherObject<Boolean> aA;
	private static DataWatcherObject<Byte> T;
	private static final EntityArmorStand eas = new EntityArmorStand(((CraftWorld) Bukkit.getWorlds().get(0)).getHandle(),
			0, 0, 0);
	static {
		try {
			Field f = Entity.class.getDeclaredField("az");
			f.setAccessible(true);
			az = (DataWatcherObject<Optional<IChatBaseComponent>>) f.get(null);
			f = Entity.class.getDeclaredField("aA");
			f.setAccessible(true);
			aA = (DataWatcherObject<Boolean>) f.get(null);
			f = Entity.class.getDeclaredField("T");
			f.setAccessible(true);
			T = (DataWatcherObject<Byte>) f.get(null);
		} catch (Exception err) {
			err.printStackTrace();
		}
	}

	private void end() {
		ended = true;
		for (LGPlayer lgp : viewers)
			showVoting(lgp, null);
		for (LGPlayer lgp : votes.keySet())
			updateVotes(lgp, true);
		int max = 0;
		boolean equal = false;
		for (Entry<LGPlayer, List<LGPlayer>> entry : votes.entrySet())
			if (entry.getValue().size() > max) {
				equal = false;
				max = entry.getValue().size();
				choosen = entry.getKey();
			} else if (entry.getValue().size() == max) {
				equal = true;
			}
		for (LGPlayer player : participants) {
			player.getCache().remove("vote");
			player.stopChoosing();
		}
		if (equal)
			choosen = null;
		if (equal && mayor == null && randomIfEqual) {
			ArrayList<LGPlayer> choosable = new ArrayList<>();
			for (Entry<LGPlayer, List<LGPlayer>> entry : votes.entrySet())
				if (entry.getValue().size() == max)
					choosable.add(entry.getKey());
			choosen = choosable.get(game.getRandom().nextInt(choosable.size()));
		}

		if (equal && mayor != null && max != 0) {
			for (LGPlayer player : viewers)
				player.sendMessage("§9Égalité, le §5§lCapitaine§9 va départager les votes.");
			mayor.sendMessage("§6Tu dois choisir qui va mourir.");

			ArrayList<LGPlayer> choosable = new ArrayList<>();
			for (Entry<LGPlayer, List<LGPlayer>> entry : votes.entrySet())
				if (entry.getValue().size() == max)
					choosable.add(entry.getKey());

			for (int i = 0; i < choosable.size(); i++) {
				LGPlayer lgp = choosable.get(i);
				showArrow(mayor, lgp, -mayor.getPlayer().getEntityId() - i);
			}

			StringJoiner sj = new StringJoiner(", ");
			for (int i = 0; i < choosable.size() - 1; i++)
				sj.add(choosable.get(0).getName());
			ArrayList<LGPlayer> blackListed = new ArrayList<>();
			for (LGPlayer player : participants)
				if (!choosable.contains(player))
					blackListed.add(player);
				else {
					VariousUtils.setWarning(player.getPlayer(), true);
				}
			mayorVote = true;
			game.wait(30, () -> {
				for (LGPlayer player : participants)
					if (choosable.contains(player))
						VariousUtils.setWarning(player.getPlayer(), false);

				for (int i = 0; i < choosable.size(); i++) {
					showArrow(mayor, null, -mayor.getPlayer().getEntityId() - i);
				}
				// Choix au hasard d'un joueur si personne n'a été désigné
				choosen = choosable.get(game.getRandom().nextInt(choosable.size()));
				callback.run();
			}, (player, secondsLeft) -> {
				timeout = secondsLeft;
				return mayor == player
						? "§6Il te reste §e" + secondsLeft + " seconde" + (secondsLeft > 1 ? "s" : "") + "§6 pour délibérer"
						: "§6Le §5§lCapitaine§6 délibère (§e" + secondsLeft + " s§6)";
			});
			mayor.choose(new LGChooseCallback() {

				@Override
				public void callback(LGPlayer choosen) {
					if (choosen != null) {
						if (blackListed.contains(choosen))
							mayor.sendMessage("§4§oCe joueur n'est pas concerné par le choix.");
						else {
							for (LGPlayer player : participants)
								if (choosable.contains(player))
									VariousUtils.setWarning(player.getPlayer(), false);

							for (int i = 0; i < choosable.size(); i++) {
								showArrow(mayor, null, -mayor.getPlayer().getEntityId() - i);
							}
							game.cancelWait();
							LGVote.this.choosen = choosen;
							callback.run();
						}
					}
				}
			});
		} else {
			game.cancelWait();
			callback.run();
		}

	}

	public LGChooseCallback getChooseCallback(LGPlayer who) {
		return new LGChooseCallback() {

			@Override
			public void callback(LGPlayer choosen) {
				if (choosen != null)
					vote(who, choosen);
			}
		};
	}

	public void vote(LGPlayer voter, LGPlayer voted) {
		if (blacklisted.contains(voted)) {
			voter.sendMessage("§cVous ne pouvez pas votre pour §7§l" + voted.getFullName() + "§c.");
			return;
		}
		if (voted == voter.getCache().get("vote"))
			voted = null;

		if (voted != null && voter.getPlayer() != null)
			votesSize++;
		if (voter.getCache().has("vote"))
			votesSize--;

		if (votesSize == participants.size() && game.getWaitTicks() > littleTimeout * 20) {
			votesSize = 999;
			game.wait(littleTimeout, initialTimeout, this::end, generator);
		}
		boolean changeVote = false;
		if (voter.getCache().has("vote")) {// On enlève l'ancien vote
			LGPlayer devoted = voter.getCache().get("vote");
			if (votes.containsKey(devoted)) {
				List<LGPlayer> voters = votes.get(devoted);
				if (voters != null) {
					voters.remove(voter);
					if (voters.isEmpty())
						votes.remove(devoted);
				}
			}
			voter.getCache().remove("vote");
			updateVotes(devoted);
			changeVote = true;
		}

		if (voted != null) {// Si il vient de voter, on ajoute le nouveau vote
			if (votes.containsKey(voted))
				votes.get(voted).add(voter);
			else
				votes.put(voted, new ArrayList<LGPlayer>(Arrays.asList(voter)));
			voter.getCache().set("vote", voted);
			updateVotes(voted);
		}

		if (voter.getPlayer() != null) {
			showVoting(voter, voted);

			String message;
			final String voterName = voter.getFullName();

			if (voted != null) {
				final String targetName = voted.getFullName();

				if (changeVote) {
					message = "§7§l" + voterName + "§6 a changé son vote pour §7§l" + targetName + "§6.";
					voter.sendMessage("§6Tu as changé de vote pour §7§l" + targetName + "§6.");
				} else {
					message = "§7§l" + voterName + "§6 a voté pour §7§l" + targetName + "§6.";
					voter.sendMessage("§6Tu as voté pour §7§l" + targetName + "§6.");
				}
			} else {
				message = "§7§l" + voterName + "§6 a annulé son vote.";
				voter.sendMessage("§6Tu as annulé ton vote.");
			}

			for (LGPlayer player : viewers)
				if (player != voter)
					player.sendMessage(message);
		}
	}

	public List<LGPlayer> getVotes(LGPlayer voted) {
		return votes.containsKey(voted) ? votes.get(voted) : new ArrayList<>(0);
	}

	private void updateVotes(LGPlayer voted) {
		updateVotes(voted, false);
	}

	private void updateVotes(LGPlayer voted, boolean kill) {
		int entityId = Integer.MIN_VALUE + voted.getPlayer().getEntityId();
		WrapperPlayServerEntityDestroy destroy = new WrapperPlayServerEntityDestroy();
		destroy.setEntityIds(new int[] { entityId });
		for (LGPlayer lgp : viewers)
			destroy.sendPacket(lgp.getPlayer());

		if (!kill) {
			int max = 0;
			for (Entry<LGPlayer, List<LGPlayer>> entry : votes.entrySet())
				if (entry.getValue().size() > max)
					max = entry.getValue().size();
			List<LGPlayer> last = latestTop;
			latestTop = new ArrayList<>();
			for (Entry<LGPlayer, List<LGPlayer>> entry : votes.entrySet())
				if (entry.getValue().size() == max)
					latestTop.add(entry.getKey());
			Bukkit.getPluginManager().callEvent(new LGVoteLeaderChange(game, this, last, latestTop));
		}

		if (votes.containsKey(voted) && !kill) {
			Location loc = voted.getPlayer().getLocation();

			WrapperPlayServerSpawnEntityLiving spawn = new WrapperPlayServerSpawnEntityLiving();
			spawn.setEntityID(entityId);
			spawn.setType(EntityType.DROPPED_ITEM);
			spawn.setX(loc.getX());
			spawn.setY(loc.getY() + 0.3);
			spawn.setZ(loc.getZ());

			int votesNbr = votes.get(voted).size();
			final int numberOfParticipants = participants.size();
			final double votePercentage = ((double) votesNbr / numberOfParticipants) * 100;
			final String votePercentageFormated = String.format("%.0f%%", votePercentage);
			final String voteContent = "§6§l" + votesNbr + " / " + numberOfParticipants + "§e vote"
					+ (votesNbr > 1 ? "s" : "") + " (§6§l" + votePercentageFormated + "§e)";

			DataWatcher datawatcher = new DataWatcher(eas);
			datawatcher.register(T, (byte) 0x20);
			datawatcher.register(az,
					Optional.ofNullable(IChatBaseComponent.ChatSerializer.a("{\"text\":\"" + voteContent + "\"}")));
			datawatcher.register(aA, true);
			PacketPlayOutEntityMetadata meta = new PacketPlayOutEntityMetadata(entityId, datawatcher, true);

			for (LGPlayer lgp : viewers) {
				spawn.sendPacket(lgp.getPlayer());
				((CraftPlayer) lgp.getPlayer()).getHandle().playerConnection.sendPacket(meta);
			}
		}
	}

	WrappedDataWatcherObject invisible = new WrappedDataWatcherObject(0, WrappedDataWatcher.Registry.get(Byte.class));
	WrappedDataWatcherObject noGravity = new WrappedDataWatcherObject(5, WrappedDataWatcher.Registry.get(Boolean.class));
	WrappedDataWatcherObject customNameVisible = new WrappedDataWatcherObject(3,
			WrappedDataWatcher.Registry.get(Boolean.class));
	WrappedDataWatcherObject customName = new WrappedDataWatcherObject(2,
			WrappedDataWatcher.Registry.get(IChatBaseComponent.class));
	WrappedDataWatcherObject item = new WrappedDataWatcherObject(7,
			WrappedDataWatcher.Registry.get(net.minecraft.server.v1_15_R1.ItemStack.class));

	private void showVoting(LGPlayer to, LGPlayer ofWho) {
		int entityId = -to.getPlayer().getEntityId();
		WrapperPlayServerEntityDestroy destroy = new WrapperPlayServerEntityDestroy();
		destroy.setEntityIds(new int[] { entityId });
		destroy.sendPacket(to.getPlayer());
		if (ofWho != null) {
			WrapperPlayServerSpawnEntityLiving spawn = new WrapperPlayServerSpawnEntityLiving();
			spawn.setEntityID(entityId);
			spawn.setType(EntityType.DROPPED_ITEM);
			Location loc = ofWho.getPlayer().getLocation();
			spawn.setX(loc.getX());
			spawn.setY(loc.getY() + 1.3);
			spawn.setZ(loc.getZ());
			spawn.setHeadPitch(0);
			Location toLoc = to.getPlayer().getLocation();
			double diffX = loc.getX() - toLoc.getX();
			double diffZ = loc.getZ() - toLoc.getZ();
			float yaw = 180 - ((float) Math.toDegrees(Math.atan2(diffX, diffZ)));

			spawn.setYaw(yaw);
			spawn.sendPacket(to.getPlayer());

			WrapperPlayServerEntityMetadata meta = new WrapperPlayServerEntityMetadata();
			meta.setEntityID(entityId);
			meta.setMetadata(Arrays.asList(new WrappedWatchableObject(invisible, (byte) 0x20),
					new WrappedWatchableObject(noGravity, true)));
			meta.sendPacket(to.getPlayer());

			WrapperPlayServerEntityLook look = new WrapperPlayServerEntityLook();
			look.setEntityID(entityId);
			look.setPitch(0);
			look.setYaw(yaw);
			look.sendPacket(to.getPlayer());

			new BukkitRunnable() {

				@Override
				public void run() {
					WrapperPlayServerEntityEquipment equip = new WrapperPlayServerEntityEquipment();
					equip.setEntityID(entityId);
					equip.setSlot(ItemSlot.HEAD);
					ItemStack skull = new ItemStack(Material.EMERALD);
					equip.setItem(skull);
					equip.sendPacket(to.getPlayer());
				}
			}.runTaskLater(MainLg.getInstance(), 2);
		}
	}

	private void showArrow(LGPlayer to, LGPlayer ofWho, int entityId) {
		WrapperPlayServerEntityDestroy destroy = new WrapperPlayServerEntityDestroy();
		destroy.setEntityIds(new int[] { entityId });
		destroy.sendPacket(to.getPlayer());
		if (ofWho != null) {
			WrapperPlayServerSpawnEntityLiving spawn = new WrapperPlayServerSpawnEntityLiving();
			spawn.setEntityID(entityId);
			spawn.setType(EntityType.DROPPED_ITEM);
			Location loc = ofWho.getPlayer().getLocation();
			spawn.setX(loc.getX());
			spawn.setY(loc.getY() + 1.3);
			spawn.setZ(loc.getZ());
			spawn.setHeadPitch(0);
			Location toLoc = to.getPlayer().getLocation();
			double diffX = loc.getX() - toLoc.getX();
			double diffZ = loc.getZ() - toLoc.getZ();
			float yaw = 180 - ((float) Math.toDegrees(Math.atan2(diffX, diffZ)));

			spawn.setYaw(yaw);
			spawn.sendPacket(to.getPlayer());

			WrapperPlayServerEntityMetadata meta = new WrapperPlayServerEntityMetadata();
			meta.setEntityID(entityId);
			meta.setMetadata(Arrays.asList(new WrappedWatchableObject(invisible, (byte) 0x20),
					new WrappedWatchableObject(noGravity, true)));
			meta.sendPacket(to.getPlayer());

			WrapperPlayServerEntityLook look = new WrapperPlayServerEntityLook();
			look.setEntityID(entityId);
			look.setPitch(0);
			look.setYaw(yaw);
			look.sendPacket(to.getPlayer());

			new BukkitRunnable() {

				@Override
				public void run() {
					WrapperPlayServerEntityEquipment equip = new WrapperPlayServerEntityEquipment();
					equip.setEntityID(entityId);
					equip.setSlot(ItemSlot.HEAD);
					ItemStack skull = new ItemStack(Material.EMERALD);
					equip.setItem(skull);
					equip.sendPacket(to.getPlayer());
				}
			}.runTaskLater(MainLg.getInstance(), 2);
		}
	}

	public void remove(LGPlayer killed) {
		participants.remove(killed);
		if (!ended) {
			votes.remove(killed);
			latestTop.remove(killed);
		}
	}
}
