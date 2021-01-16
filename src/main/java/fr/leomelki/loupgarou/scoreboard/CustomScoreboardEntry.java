package fr.leomelki.loupgarou.scoreboard;

import java.util.Arrays;

import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.EnumWrappers.ScoreboardAction;

import fr.leomelki.com.comphenix.packetwrapper.WrapperPlayServerScoreboardScore;
import fr.leomelki.com.comphenix.packetwrapper.WrapperPlayServerScoreboardTeam;
import fr.leomelki.loupgarou.classes.LGPlayer;

public class CustomScoreboardEntry {
	private final int amount;
	private final CustomScoreboard scoreboard;
	private final String name;
	private final String scoreboardName;
	private WrappedChatComponent prefix;

	public CustomScoreboardEntry(CustomScoreboard scoreboard, String rawName, int amount) {
		this.amount = amount;
		this.scoreboard = scoreboard;
		this.scoreboardName = scoreboard.getName();
		this.name = this.generateDisplayableName(rawName);
		this.show();
	}

	public void show() {
		WrapperPlayServerScoreboardTeam team = new WrapperPlayServerScoreboardTeam();
		team.setPlayers(Arrays.asList(this.name));
		team.setPrefix(this.prefix);
		team.setName(this.name);
		team.setMode(0);

		WrapperPlayServerScoreboardScore score = new WrapperPlayServerScoreboardScore();
		score.setScoreboardAction(ScoreboardAction.CHANGE);
		score.setObjectiveName(this.scoreboardName);
		score.setScoreName(name);
		score.setValue(this.amount);

		for (LGPlayer current : this.scoreboard.getInGamePlayers()) {
			team.sendPacket(current.getPlayer().getPlayer());
			score.sendPacket(current.getPlayer().getPlayer());
		}
	}

	public String generateDisplayableName(String rawName) {
		if (rawName.length() <= 16) {
			return rawName;
		}

		int limit = 16;

		if (rawName.charAt(15) == '§') {
			limit = 15;
		} else if (rawName.charAt(14) == '§' && rawName.charAt(13) != '§') {
			limit = 14;
		}

		final String sringifiedPrefix = rawName.substring(0, limit);
		String suffix;

		if (limit != 16) {
			suffix = rawName.substring(limit);
		} else {
			char colorCode = 'f';
			boolean storeColorCode = false;
			for (char c : sringifiedPrefix.toCharArray()) {
				if (storeColorCode) {
					storeColorCode = false;
					colorCode = c;
				} else if (c == '§') {
					storeColorCode = true;
				}
			}
			suffix = "§" + colorCode + rawName.substring(limit);
		}

		this.prefix = WrappedChatComponent.fromText(sringifiedPrefix);

		return suffix;
	}

	public void delete() {
		hide();
	}

	public void hide() {
		if (scoreboard.isShown()) {
			WrapperPlayServerScoreboardScore score = new WrapperPlayServerScoreboardScore();
			score.setObjectiveName(scoreboard.getName());
			score.setScoreboardAction(ScoreboardAction.REMOVE);
			score.setScoreName(this.name);
			score.setValue(this.amount);

			WrapperPlayServerScoreboardTeam team = new WrapperPlayServerScoreboardTeam();
			team.setName(this.name);
			team.setMode(1);

			for (LGPlayer current : this.scoreboard.getInGamePlayers()) {
				team.sendPacket(current.getPlayer().getPlayer());
				score.sendPacket(current.getPlayer().getPlayer());
			}
		}
	}

}
