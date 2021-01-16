package fr.leomelki.loupgarou.events;

import org.bukkit.event.Cancellable;

import fr.leomelki.loupgarou.classes.LGGame;
import lombok.Getter;
import lombok.Setter;

public class LGDayStartEvent extends LGEvent implements Cancellable {
	@Getter @Setter private boolean cancelled;

	public LGDayStartEvent(LGGame game) {
		super(game);
	}
}