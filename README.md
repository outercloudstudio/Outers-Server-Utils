# Simple Entity Respawns
The purpose of this mob is to be able to create respawnable groups of entities based on tags. Simple Entity Respawns is completely server sided!

## Tutorial
First create a respawn group by running the command `/respawns create my_respawn_group 1 5`. This command will create a respawn group linked to the tag `my_respawn_group` with a delay of `1` second and radius of `5`.

> [!IMPORTANT]  
> Create the respawn group before adding entities to the group!

## Commands
### `/respawns create <tag: string> <delay: float> <radius: float>`
Creates a group assigned to a specific tag. `delay` is a float is seconds. Entities wait this amount of time before respawning. `radius` is a float in blocks. Entities that travel farther than this distance from when they were given the tag will be teleported back to their original spot.

### `/respawns remove <tag: string>`
Removes the respawn group assigned to the tag.

### `/respawns edit <tag: string> <delay: float> <radius: float>`
Modifies the attributes of the respawn group. Parameters are the same as `create`.

### `/respawns list`
Lists out the tags of the currently existing respawn groups.

# Download
Modrinth:
https://modrinth.com/mod/blue-cloud-simple-npcs

Curseforge:
https://legacy.curseforge.com/minecraft/mc-mods/bluecloud-simple-npcs
