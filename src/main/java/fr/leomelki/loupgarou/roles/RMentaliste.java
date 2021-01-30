package fr.leomelki.loupgarou.roles;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import fr.leomelki.loupgarou.classes.LGGame;
import fr.leomelki.loupgarou.classes.LGPlayer;
import fr.leomelki.loupgarou.classes.LGPlayer.LGChooseCallback;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

public class RMentaliste extends Role {
	private static final String IN_CONNECTION = "in_connection";

	public RMentaliste(LGGame game) {
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
		return this.getName();
	}

	@Override
	public String getName() {
		return "§a§lMentaliste";
	}

	@Override
	public String getFriendlyName() {
		return "des " + getName();
	}

	@Override
	public String getShortDescription() {
		return "Tu gagnes avec le §a§lVillage";
	}

	@Override
	public String getDescription() {
		return "Tu gagnes avec le §a§lVillage§f. Tu es mentaliste casse pas les couilles";//TODO Changer la description
	}

	@Override
	public String getTask() {
		return "Choisir un joueur avec lequel vous souhaitez communiquer AVEC §l(@message)";
	}

	@Override
	public String getBroadcastedTask() {
		return "Le " + getName() + "§9 s'apprête à établir une connexion avec quelqu'un...";
	}

	@Override
	protected void onNightTurn(LGPlayer player, Runnable callback) {
		player.showView();
		player.choose(new LGChooseCallback() {
			@Override
			public void callback(LGPlayer choosen) {
				if (choosen != null && choosen != player) {
					setInConnection(player,choosen);
					player.stopChoosing();
					player.hideView();
					callback.run();
				}
			}
		});
	}

	@Override
	protected void onNightTurnTimeout(LGPlayer player) {
		player.stopChoosing();
		player.hideView();
	}

	@Override
	public int getTimeout() {
		return 15;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onChat(AsyncPlayerChatEvent e) {
		LGPlayer player = LGPlayer.thePlayer(e.getPlayer());
		if (player.getGame() == getGame()) {
			if (e.getMessage().startsWith("@")) {
				if (player.getCache().has(RMentaliste.IN_CONNECTION)) {
					player.sendMessage("§d\u2764 " + player.getFullName() + " §6» §f" + e.getMessage().substring(1));
					player.getCache().<LGPlayer>get(RMentaliste.IN_CONNECTION).sendMessage("§d\u2764 " + player.getFullName() + " §6» §f" + e.getMessage().substring(1));
				} else {
					player.sendMessage("§4Erreur : §cVous n'êtes pas connecté !");
				}
				e.setCancelled(true);
			}
		}
	}

	protected void setInConnection(LGPlayer player1, LGPlayer player2) {
		player1.getCache().set(RMentaliste.IN_CONNECTION, player2);
		player1.sendMessage("§9La connexion est établi avec §7§l" + player2.getFullName() + ".");
		player1.sendMessage("§9§oTu peux lui parler en mettant un §e@§9 devant ton message.");

		player2.getCache().set(RMentaliste.IN_CONNECTION, player1);
		player2.sendMessage("§9Le mentaliste est entré dans ton esprit GG a lui ! On peut créer des cheats grâce à ça");
		player2.sendMessage("§9§oTu peux lui parler en mettant un §e@§9 devant ton message.");

		// On peut créer des cheats grâce à ça (qui permettent de savoir qui est en couple)
		player1.updatePrefix();
		player2.updatePrefix();
	}
}
