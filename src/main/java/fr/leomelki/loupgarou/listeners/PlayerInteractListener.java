package fr.leomelki.loupgarou.listeners;

import fr.leomelki.loupgarou.MainLg;
import fr.leomelki.loupgarou.roles.Role;
import fr.leomelki.loupgarou.utils.ItemBuilder;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

public class PlayerInteractListener implements Listener {
    @Getter private Map<String, Constructor<? extends Role>> roles = new HashMap<>();

    public PlayerInteractListener(Map<String, Constructor<? extends Role>> roles) {
        this.roles = roles;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        int index = 0;
        ItemStack item = e.getItem();
        if (item == null)
            return;

        if (item.getType().equals(Material.SEA_PICKLE)) {
            Player p = e.getPlayer();

            Inventory gui = Bukkit.createInventory(null, 4 * 9, "Rôles");
            for (String role : getRoles().keySet())
                gui.setItem(index++, new ItemBuilder(Material.HEART_OF_THE_SEA).setName(role).build());
            gui.setItem(35, new ItemBuilder(Material.GOLD_NUGGET).setName("Valider").build());
            p.openInventory(gui);
        } else if (item.getType().equals(Material.EMERALD)) {
            Bukkit.dispatchCommand(e.getPlayer(), "lg start " + e.getPlayer().getDisplayName());
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        ItemStack item = e.getCurrentItem();
        if (item == null)
        {
            return;
        }
        if (e.getView().getTitle().equals("Rôles")) {
            int index = 0;
            Integer n = null;

            if (e.getCurrentItem().getType() == Material.GOLD_NUGGET) {
                for (Player pl : Bukkit.getOnlinePlayers())
                    Bukkit.getPluginManager().callEvent(new PlayerQuitEvent(pl, "joinall"));
                for (Player pl : Bukkit.getOnlinePlayers())
                    Bukkit.getPluginManager().callEvent(new PlayerJoinEvent(pl, "joinall"));
                p.getInventory().setItem(3, new ItemBuilder(Material.EMERALD).setName("Lancer la partie").build());
                p.getInventory().setItem(1, new ItemBuilder(Material.SEA_PICKLE).setName("Choisir les rôles").build());
            } else if (e.isLeftClick()) {
                for (String role : getRoles().keySet()) {
                    if (role.equals(e.getCurrentItem().getItemMeta().getDisplayName())) {
                        n = MainLg.getInstance().getConfig().getInt("role." + role);
                        // Bukkit.getLogger().info("role." + n);
                        Bukkit.dispatchCommand(p, "lg roles set " + index + " " + (n + 1));
                        return;
                    }
                    index++;
                }
            } else if (e.isRightClick()) {
                for (String role : getRoles().keySet()) {
                    if (role.equals(e.getCurrentItem().getItemMeta().getDisplayName())) {
                        n = MainLg.getInstance().getConfig().getInt("role." + role);
                        if (n > 0)
                            Bukkit.dispatchCommand(p, "lg roles set " + index + " " + (n - 1));
                        return;
                    }
                    index++;
                }
            }
        }
    }
}