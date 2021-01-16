package fr.leomelki.loupgarou.events;

import fr.leomelki.loupgarou.classes.LGGame;
import fr.leomelki.loupgarou.classes.LGWinType;
import lombok.Getter;
import lombok.Setter;

public class LGEndCheckEvent extends LGEvent {
	@Getter @Setter private LGWinType winType;

	public LGEndCheckEvent(LGGame game, LGWinType winType) {
		super(game);
		this.winType = winType;
	}
}