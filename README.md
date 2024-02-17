# Simple NPCs
The purpose of this mob is to be able to create respawnable NPC groups or individuals. Simple NPCs is completely server sided!

## Idea
1. Summon entity and modify it to desired effect
2. Select entity/entities
3. Create spawn group
4. Entities will now respawn

## Technical
NPCs are saved using NBT so anything that works with NBT should work with this mod.

Groups are based off of tags so the `/tag` command can be used to completely skip using the `/select` command.

## Commands
`/respawn` allows the creation, deletion, and modification of entity respawn groups.

`/respawn create` allows you to specify a tag. All entities with that tag will be assigned to the respawn group. You can specify the delay in seconds between killing an entity before it respawns, and the max amount of entities that will be spawned at any one time. You can also specify if groups will randomly pick from the pool or always respawn the same mobs.

`/respawn remove` lets you remove the specified respawn group.

`/respawn edit` lets you modify the delay and amount of entities in the specified respawn group.

`/respawn list` lists out the names of the currently existing respawn groups

`/respawn reset` respawns all the entities in a respawn group, or all respawn groups without dropping any loot

`/respawn freeze` prevents entities in a respawn group or all respawn groups from spawning.

`/respawn unfreeze` unfreezes all or a specific respawn group.

# Download
Modrinth:
https://modrinth.com/mod/blue-cloud-simple-npcs

Curseforge:
https://legacy.curseforge.com/minecraft/mc-mods/bluecloud-simple-npcs
