package fr.leomelki.loupgarou.roles;

import java.util.Arrays;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_15_R1.inventory.CraftInventoryCustom;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import fr.leomelki.loupgarou.MainLg;
import fr.leomelki.loupgarou.classes.LGGame;
import fr.leomelki.loupgarou.classes.LGPlayer;
import fr.leomelki.loupgarou.classes.LGWinType;
import fr.leomelki.loupgarou.events.LGGameEndEvent;
import fr.leomelki.loupgarou.events.LGNightPlayerPreKilledEvent;
import fr.leomelki.loupgarou.events.LGPlayerKilledEvent.Reason;
import fr.leomelki.loupgarou.events.LGPreDayStartEvent;
import fr.leomelki.loupgarou.events.LGVampiredEvent;
import fr.leomelki.loupgarou.utils.VariableCache;

public class RSurvivant extends Role {
	protected static final String AMOUNT_OF_PROTECTIONS_REMAINING = "2";
	private static final String IMMUNITY_FROM_WOLVES = "immunity_from_wolves_survivor";

	public RSurvivant(LGGame game) {
		super(game);
	}

	@Override
	public RoleType getType() {
		return RoleType.NEUTRAL;
	}

	@Override
	public RoleWinType getWinType() {
		return RoleWinType.NONE;
	}

	@Override
	public String getName(int amount) {
		final String baseline = this.getName();

		return (amount > 1) ? baseline + "s" : baseline;
	}

	@Override
	public String getName() {
		return "§d§lSurvivant";
	}

	@Override
	public String getFriendlyName() {
		return "du " + getName();
	}

	@Override
	public String getShortDescription() {
		return "Tu gagnes si tu remplis ton objectif";
	}

	@Override
	public String getDescription() {
		return "Tu es §d§lNeutre§f et tu gagnes si tu remplis ton objectif. Ton objectif est de survivre. Tu disposes de §l2§f protections. Chaque nuit, tu peux utiliser une protection pour ne pas être tué par les §c§lLoups§f. Tu peux gagner aussi bien avec les §a§lVillageois§f qu'avec les §c§lLoups§f, tu dois juste rester en vie jusqu'à la fin de la partie.";
	}

	@Override
	public String getTask() {
		return "Veux-tu utiliser une protection cette nuit ?";
	}

	@Override
	public String getBroadcastedTask() {
		return "Le " + getName() + "§9 décide s'il veut se protéger.";
	}

	@Override
	public int getTimeout() {
		return 15;
	}

	boolean inMenu;

	public void openInventory(Player player) {
		inMenu = true;
		Inventory inventory = Bukkit.createInventory(null, 9, "§7Veux-tu te protéger ?");
		ItemStack[] items = new ItemStack[9];
		LGPlayer lgp = LGPlayer.thePlayer(player);
		if (lgp.getCache().has(RSurvivant.AMOUNT_OF_PROTECTIONS_REMAINING) && lgp.getCache().get(RSurvivant.AMOUNT_OF_PROTECTIONS_REMAINING) != "0") {
			items[3] = new ItemStack(Material.IRON_NUGGET);
			ItemMeta meta = items[3].getItemMeta();
			meta.setDisplayName("§7§lNe rien faire");
			meta.setLore(Arrays.asList("§8Passez votre tour"));
			items[3].setItemMeta(meta);
			items[5] = new ItemStack(Material.GOLD_NUGGET);
			meta = items[5].getItemMeta();
			meta.setDisplayName(
					"§2§lSe protéger (§6§l" + lgp.getCache().get(RSurvivant.AMOUNT_OF_PROTECTIONS_REMAINING) + "§2§l restant)");
			meta.setLore(Arrays.asList("§8Tu ne pourras pas être tué par", "§8  les §c§lLoups§8 cette nuit."));
			items[5].setItemMeta(meta);
		} else {
			items[4] = new ItemStack(Material.IRON_NUGGET);
			ItemMeta meta = items[4].getItemMeta();
			meta.setDisplayName("§7§lNe rien faire");
			meta.setLore(Arrays.asList("§8Passez votre tour"));
			items[4].setItemMeta(meta);
		}
		player.closeInventory();
		inventory.setContents(items);
		player.openInventory(inventory);
	}

	@Override
	public void join(LGPlayer player,boolean sendMessage) {
		super.join(player,sendMessage);
		player.getCache().set(RSurvivant.AMOUNT_OF_PROTECTIONS_REMAINING, "2");
	}

	Runnable callback;

	@Override
	protected void onNightTurn(LGPlayer player, Runnable callback) {
		player.showView();
		this.callback = callback;
		openInventory(player.getPlayer());
	}

	@Override
	protected void onNightTurnTimeout(LGPlayer player) {
		player.hideView();
		closeInventory(player.getPlayer());
		player.sendMessage("§4§oTu es sans défense...");
	}

	private void closeInventory(Player p) {
		inMenu = false;
		p.closeInventory();
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent e) {
		ItemStack item = e.getCurrentItem();
		Player player = (Player) e.getWhoClicked();
		LGPlayer lgp = LGPlayer.thePlayer(player);

		if (lgp.getRole() != this || item == null || item.getItemMeta() == null)
			return;

		if (item.getType() == Material.IRON_NUGGET) {
			e.setCancelled(true);
			lgp.sendMessage("§4§oTu es sans défense...");
			closeInventory(player);
			lgp.hideView();
			callback.run();
		} else if (item.getType() == Material.GOLD_NUGGET) {
			e.setCancelled(true);
			closeInventory(player);
			lgp.sendActionBarMessage("§9§lTu as décidé de te protéger.");
			lgp.sendMessage("§6Tu as décidé de te protéger.");
			Integer AOPR = Integer.parseInt(lgp.getCache().get(RSurvivant.AMOUNT_OF_PROTECTIONS_REMAINING)) - 1;
			if(AOPR == 0)
			{
				lgp.getCache().remove(RSurvivant.AMOUNT_OF_PROTECTIONS_REMAINING);
			} 
			else
			{
				lgp.getCache().set(RSurvivant.AMOUNT_OF_PROTECTIONS_REMAINING,String.valueOf(AOPR));
			}
			
			lgp.getCache().set(RSurvivant.IMMUNITY_FROM_WOLVES, true);
			lgp.hideView();
			callback.run();
		}
	}

	@EventHandler
	public void onPlayerKill(LGNightPlayerPreKilledEvent e) {
		if (e.getGame() == getGame()
				&& (e.getReason() == Reason.LOUP_GAROU || e.getReason() == Reason.LOUP_BLANC
						|| e.getReason() == Reason.GM_LOUP_GAROU || e.getReason() == Reason.ASSASSIN)
				&& e.getKilled().getCache().has(RSurvivant.IMMUNITY_FROM_WOLVES) && e.getKilled().isRoleActive()) {
			e.setReason(Reason.DONT_DIE);
		}
	}

	@EventHandler
	public void onVampired(LGVampiredEvent e) {
		if (e.getGame() == getGame() && e.getPlayer().getCache().has(RSurvivant.IMMUNITY_FROM_WOLVES))
			e.setProtect(true);
	}

	@EventHandler
	public void onDayStart(LGPreDayStartEvent e) {
		if (e.getGame() == getGame())
			for (LGPlayer lgp : getGame().getInGame())
				if (lgp.isRoleActive())
					lgp.getCache().remove(RSurvivant.IMMUNITY_FROM_WOLVES);
	}

	@EventHandler
	public void onQuitInventory(InventoryCloseEvent e) {
		if (e.getInventory() instanceof CraftInventoryCustom) {
			LGPlayer player = LGPlayer.thePlayer((Player) e.getPlayer());
			if (player.getRole() == this && inMenu) {
				new BukkitRunnable() {

					@Override
					public void run() {
						e.getPlayer().openInventory(e.getInventory());
					}
				}.runTaskLater(MainLg.getInstance(), 1);
			}
		}
	}

	@EventHandler
	public void onWin(LGGameEndEvent e) {
		if (e.getGame() == getGame() && !getPlayers().isEmpty() && e.getWinType() != LGWinType.ANGE) {
			for (LGPlayer lgp : getPlayers())
				e.getWinners().add(lgp);
			new BukkitRunnable() {

				@Override
				public void run() {
					getGame().broadcastMessage("§6§oLe " + getName() + "§6§o a rempli son objectif.");
				}
			}.runTaskAsynchronously(MainLg.getInstance());
		}
	}
}
