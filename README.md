# Basic Npcs
The purpose of this mob is to be able to create respawnable NPC groups or individuals.

## Idea
1. Summon entity and modify it to desired effect
2. Select entity/entities
3. Create spawn group
4. Entities will now respawn

## Techical
NPCs are saved using NBT so anything that works with NBT should work with this mod.

Groups are based off of tags so the `/tag` command can be used to completely skip using the `/select` command.

## Commands
`/select` allows you to select mobs easily.

This command adds the `selected` tag to the specified entities.

By default, this command targets the entity in front of the player, otherwise you can use `/select box` and its corresponding sub commands to select entities in an area.

---

`/deselect` works nearly identical to the `/select` command except it include the `/deselect all` sub command that will deselect all entities.

---

`/respawn` allows the creation, deletion, and modification of entity respawn groups.

`/respawn create` allows you to specify a tag. All entities with that tag will be assigned to the respawn group. You can specify the delay in seconds between killing an entity before it respawns, and the max amount of entities that will be spawned at any one time.

`/respawn remove` lets you remove the specified respawn group.

`/respawn edit` lets you modify the delay and amount of entities in the specified respawn group.

`/respawn list` lists out the names of the currently existing respawn groups

## Tutorial
1. Find an entity. You can spawn one using `/summon` or using a spawn egg.
2. Run `/select` while looking directly at the entity. You should see a glowing soul above the entity after selecting it, otherwise it isn't selected.
3. Run the command `/tag @e[tag=selected] add my_respawn_group` You should change `my_respawn_group` to a different name you will rememeber.
4. Run `/respawn my_respawn_group` or change `my_respawn_group` to the tag name you put in earlier.

You should now see the mob will respawn when killed!