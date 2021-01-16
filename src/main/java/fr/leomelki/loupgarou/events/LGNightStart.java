package fr.leomelki.loupgarou.events;

import org.bukkit.event.Cancellable;

import fr.leomelki.loupgarou.classes.LGGame;
import lombok.Getter;
import lombok.Setter;

public class LGNightStart extends LGEvent implements Cancellable {
	@Getter @Setter boolean cancelled;

	public LGNightStart(LGGame game) {
		super(game);
	}
}
