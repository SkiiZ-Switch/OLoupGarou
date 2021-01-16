package fr.leomelki.loupgarou.scoreboard;

import java.util.ArrayList;
import java.util.List;

import com.comphenix.protocol.wrappers.WrappedChatComponent;

import fr.leomelki.com.comphenix.packetwrapper.WrapperPlayServerScoreboardDisplayObjective;
import fr.leomelki.com.comphenix.packetwrapper.WrapperPlayServerScoreboardObjective;
import fr.leomelki.loupgarou.classes.LGPlayer;
import fr.leomelki.loupgarou.classes.RolePlayers;
import fr.leomelki.loupgarou.roles.Role;
import fr.leomelki.loupgarou.utils.RandomString;
import lombok.Getter;

public class CustomScoreboard {
	@Getter private boolean shown;
	@Getter private final String name = RandomString.generate(15);
	@Getter private final List<LGPlayer> inGamePlayers;
	private static final String DISPLAY_NAME = "§7";
	private final List<CustomScoreboardEntry> entries = new ArrayList<>();
	private final boolean shouldShowScoreboard;

	public CustomScoreboard(List<LGPlayer> inGamePlayers, boolean shouldShowScoreboard) {
		this.inGamePlayers = inGamePlayers;
		this.shouldShowScoreboard = shouldShowScoreboard;
	}

	private void createEntry(String name, int amountOfPlayers) {
		this.entries.add(new CustomScoreboardEntry(this, name, amountOfPlayers));
	}

	private void removePreexistingEntries() {
		final List<CustomScoreboardEntry> preexistingEntries = new ArrayList<>(this.entries);

		for (CustomScoreboardEntry preexistingEntry : preexistingEntries) {
			preexistingEntry.hide();
			this.entries.remove(preexistingEntry);
		}
	}

	public void displayEntries(List<RolePlayers> activeRoles) {
		this.removePreexistingEntries();
		int totalRemaingPlayers = 0;

		for (RolePlayers currentPlayers : activeRoles) {
			final int amountOfPlayers = currentPlayers.getAmountOfPlayers();

			if (amountOfPlayers > 0) {
				if (this.shouldShowScoreboard) {
					final Role currentRole = currentPlayers.getRole();
					final String sanitizedName = currentRole.getName(amountOfPlayers).replace("§l", "");

					this.createEntry(sanitizedName, amountOfPlayers);
				}

				totalRemaingPlayers += amountOfPlayers;
			}
		}

		this.createEntry("§e[TOTAL]", totalRemaingPlayers);
	}

	public void announce(String message, int fakeDuration) {
		this.removePreexistingEntries();
		this.createEntry(message, fakeDuration);
	}

	public void show() {
		WrapperPlayServerScoreboardObjective objective = new WrapperPlayServerScoreboardObjective();
		objective.setMode(0);
		objective.setName(name);
		objective.setDisplayName(WrappedChatComponent.fromText(DISPLAY_NAME));

		WrapperPlayServerScoreboardDisplayObjective display = new WrapperPlayServerScoreboardDisplayObjective();
		display.setPosition(1);
		display.setScoreName(name);

		for (LGPlayer currentPlayer : inGamePlayers) {
			objective.sendPacket(currentPlayer.getPlayer());
			display.sendPacket(currentPlayer.getPlayer());
		}

		shown = true;
	}

	public void hide() {
		WrapperPlayServerScoreboardObjective remove = new WrapperPlayServerScoreboardObjective();
		remove.setMode(1);
		remove.setName(name);

		for (LGPlayer currentPlayer : inGamePlayers) {
			remove.sendPacket(currentPlayer.getPlayer());
		}

		this.removePreexistingEntries();
		shown = false;
	}
}
