package fr.leomelki.loupgarou.roles;

import fr.leomelki.loupgarou.classes.LGGame;
import fr.leomelki.loupgarou.classes.LGPlayer;
import fr.leomelki.loupgarou.classes.LGPlayer.LGChooseCallback;

public class RDetective extends Role {
	private static final String DETECTIVE_FIRST = "detective_first";

	public RDetective(LGGame game) {
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
		return "§a§lDétective";
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
		return "Tu gagnes avec le §a§lVillage§f. Chaque nuit, tu mènes l'enquête sur deux joueurs pour découvrir s'ils font partie du même camp.";
	}

	@Override
	public String getTask() {
		return "Choisis deux joueurs à étudier.";
	}

	@Override
	public String getBroadcastedTask() {
		return "Le " + getName() + "§9 est sur une enquête...";
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
				if (choosen != null) {
					if (choosen == player) {
						player.sendMessage("§cVous ne pouvez pas vous sélectionner !");
						return;
					}
					if (player.getCache().has(RDetective.DETECTIVE_FIRST)) {
						LGPlayer first = player.getCache().remove(RDetective.DETECTIVE_FIRST);
						if (first == choosen) {
							player.sendMessage("§cVous ne pouvez pas comparer §7§l" + first.getFullName() + "§c avec lui même !");
						} else {
							if ((first.getRoleType() == RoleType.NEUTRAL || choosen.getRoleType() == RoleType.NEUTRAL)
									? first.getRole().getClass() == choosen.getRole().getClass()
									: first.getRoleType() == choosen.getRoleType())
								player.sendMessage(
										"§7§l" + first.getFullName() + "§6 et §7§l" + choosen.getFullName() + "§6 sont §adu même camp.");
							else
								player.sendMessage("§7§l" + first.getFullName() + "§6 et §7§l" + choosen.getFullName()
										+ "§6 ne sont §cpas du même camp.");

							player.stopChoosing();
							player.hideView();
							callback.run();
						}
					} else {
						player.getCache().set(RDetective.DETECTIVE_FIRST, choosen);
						player.sendMessage(
								"§9Choisis un joueur avec qui tu souhaites comparer le rôle de §7§l" + choosen.getFullName());
					}
				}
			}
		});
	}

	@Override
	protected void onNightTurnTimeout(LGPlayer player) {
		player.getCache().remove(RDetective.DETECTIVE_FIRST);
		player.stopChoosing();
		player.hideView();
	}
}
