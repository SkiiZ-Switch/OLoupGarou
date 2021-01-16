package fr.leomelki.loupgarou.events;

import org.bukkit.event.Cancellable;

import fr.leomelki.loupgarou.classes.LGGame;
import fr.leomelki.loupgarou.classes.LGPlayer;
import fr.leomelki.loupgarou.events.LGPlayerKilledEvent.Reason;
import lombok.Getter;
import lombok.Setter;

public class LGNightPlayerPreKilledEvent extends LGEvent implements Cancellable {
	@Getter @Setter boolean cancelled;
	@Getter private final LGPlayer killed;
	@Getter @Setter private Reason reason;

	public LGNightPlayerPreKilledEvent(LGGame game, LGPlayer killed, Reason reason) {
		super(game);
		this.killed = killed;
		this.reason = reason;
	}
}
