package fr.leomelki.loupgarou.classes;

import fr.leomelki.loupgarou.roles.Role;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RolePlayers {
	@Getter private final Role role;
	@Getter private int amountOfPlayers = 1;

	public void increment() {
		amountOfPlayers++;
	}
}
