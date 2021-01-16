## Fork of leomelki/LoupGarou by TheOptimisticFactory

- This repository is based off https://github.com/leomelki/LoupGarou and contains improvements made by the community before it lands in the main repository (if ever).
- The `dev` branch is **STABLE**, despite his name. The `wip` branch, on the other hand might not


## Server files (plug and play)

- The server files containing the compiled jar of my fork can be found at: https://github.com/TheOptimisticFactory/java-minecraft-loupGarou-server
- The jar itself is downloadable from https://github.com/TheOptimisticFactory/java-minecraft-loupGarou-server/tree/master/plugins (dont forget the [config file](https://github.com/TheOptimisticFactory/java-minecraft-loupGarou-server/blob/master/plugins/LoupGarou/config.yml))

## Additional features compared to original plugin ##

As of today (2020/05/11), the original repository version is [v1.1.0](https://github.com/leomelki/LoupGarou/releases/tag/1.1.0), released on 2020/04/07.

My repository includes the **same content** along with the following **additions** (each link contains screenshots):

- [fork v1.2.0](https://github.com/TheOptimisticFactory/LoupGarou/releases/tag/v1.2.0) - Village composition showcase at the end of the game
- [fork v1.3.0](https://github.com/TheOptimisticFactory/LoupGarou/releases/tag/v1.3.0) - Ability to set nicknames to players + GUI to configure roles and start game
- [fork v1.4.0](https://github.com/TheOptimisticFactory/LoupGarou/releases/tag/v1.4.0) - Customizable memes messages at the start of the game + Highlight of the % of votes on a given player
- [fork v1.5.0](https://github.com/TheOptimisticFactory/LoupGarou/releases/tag/v1.5.0) - Revamped scoreboard to avoid useless scoring + Server logs when a player dies or gets resurrected
- [fork v1.6.0](https://github.com/TheOptimisticFactory/LoupGarou/releases/tag/v1.6.0) - Persisted round results for postgame analytics in CSV format
- [fork v1.7.0](https://github.com/TheOptimisticFactory/LoupGarou/releases/tag/v1.7.0) - Ability to hide the scoreboard to enable role bluffing
- [fork v1.8.0](https://github.com/TheOptimisticFactory/LoupGarou/releases/tag/v1.8.0) - Ability to randomize role attribution
- [fork v1.9.0](https://github.com/TheOptimisticFactory/LoupGarou/releases/tag/v1.9.0) - Revamped command parser + revamped codebase to ease adding new roles
- [fork v1.10.0](https://github.com/TheOptimisticFactory/LoupGarou/releases/tag/v1.10.0) - Added commands to showcase random distribution

## Useful commands (for ops) ##

- `/lg joinAll` to make everyone connected join the lobby
- `/lg start` to start the game
- `/lg end` to interrupt an ongoing game
- `/lg addSpawn` to add a spawn-point on your EXACT position and look direction
- `/lg roles` to get the list of currently active roles
- `/lg roles list` to get the complete list of available roles
- `/lg roles set <role> <amount>` to set the number of players for a given role

##### Additonal commands compared to baseline repository:

- `/lg nick <username> <nickname>` to set a nickname to a player
- `/lg unnick <username>` to remove a nickname from a player
- `/lg random` to list the probability to picking each role with a weight > 0
- `/lg random showAll` to list the probability to picking each role (disregarding their weigth)
- `/lg random players <amount>` to set the number of players when using random role distribution

## Additional features gallery

![image](https://user-images.githubusercontent.com/2607260/79672340-4260a780-81d1-11ea-9b49-266a992e872a.png)

![image](https://user-images.githubusercontent.com/2607260/79674319-56f96b80-81e2-11ea-87ef-d4bdfd4494aa.png)

![javaw_Nk1NdY7KXw](https://user-images.githubusercontent.com/2607260/79673723-8e651980-81dc-11ea-8258-eb077bca7fca.png)

![image](https://user-images.githubusercontent.com/2607260/79676799-f706c300-81e9-11ea-86cd-0c9cd98be0b3.png)

![javaw_3n08F7Wy4V](https://user-images.githubusercontent.com/2607260/80318956-faafd080-880d-11ea-8a82-5d7a63f66330.png)

![image](https://user-images.githubusercontent.com/2607260/80097236-41ca6700-856b-11ea-978c-dd658ad09c67.png)

## Notes for compilation ##

- The following compilation warning is normal and can safely be ignored: `WARNING: Illegal reflective access by com.comphenix.net.sf.cglib.core.ReflectUtils$1 (file:<path>) to method java.lang.ClassLoader.defineClass(java.lang.String,byte[],int,int,java.security.ProtectionDomain)`. It is due to the fact that [ProtocolLib currently does not support Java versions above Java 8 and won't until Mojang and Spigot decide to update.](https://github.com/dmulloy2/ProtocolLib/issues/603#issuecomment-490207994)
