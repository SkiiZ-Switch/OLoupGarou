package fr.leomelki.loupgarou.events;

import fr.leomelki.loupgarou.classes.LGGame;
import fr.leomelki.loupgarou.classes.LGPlayer;
import lombok.Getter;
import lombok.Setter;

public class LGVampiredEvent extends LGEvent {
	@Getter @Setter private boolean immuned;
	@Getter @Setter private boolean protect;
	@Getter @Setter private LGPlayer player;

	public LGVampiredEvent(LGGame game, LGPlayer player) {
		super(game);
		this.player = player;
	}
}