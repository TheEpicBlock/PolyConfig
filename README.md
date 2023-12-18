# PolyConfig
PolyConfig is a mod allowing you to configure [PolyMc](https://github.com/TheEpicBlock/PolyMc). 
It uses [PolyMc's api](https://theepicblock.github.io/PolyMc/api/) internally.

```groovy
version 1

// Just mentioning a block will fill in the defaults for it, effectively prioritizing it
block "test:prioritized_block"

// Simple replacement
block "test:test_block" {
    replace "minecraft:glass"
}

// Simple replacement with a specific state
block "test:test_block2" {
    replace "minecraft:redstone_lamp" lit=true
}

// Unless specified otherwise, properties from the modded block will be copied over into the vanilla block
block "test:rope_ladder" {
    replace "minecraft:ladder"
}

// Pick from a specific group. The first one entered will be tried first
block "test:other_block" {
    replace (group)"noteblock"
    replace (group)"tripwire"
}

// You can match blocks with a regex
// Blocks that are explicitely declared will always override those declared with a regex
block "test:(other_)?block" {
    replace "stone"
}

// You can filter that specific group (TODO)
block "test:some_other_block" {
    replace (group)"leaves" waterlogged=false distance="5.."
}

block "test:some_kind_of_plant" {
    // You can merge specific values to avoid generating a block for all of them. You can use the argument to specify which one of these is the canonical one, which will be used to retrieve the resources.
    merge age="0..2"
    merge age="3.." 6
    // You can use any regex for string type properties. * will also be recognized
    merge direction="*"
    // Eliding any "replace" statement will leave PolyMc to generate it
}

// You can specify different replace entries per source state.
block "test:my_slab" {
    state type="top|double" {
        replace "minecraft:white_stained_glass"
    }
    state type="bottom" {
        replace (group)"tripwire"
    }
}

// You can also configure entities
entity "test:my_test_entity" {
    base "minecraft:zombie"
    name "yeet"
}

entity "test:my_test_entity_2" {
    base "minecraft:zombie"
    name null
}

// And you can also configure items, if you need to
// NOTE: This is not implemented yet!
item "test:magic_sword" {
    replacement "minecraft:diamond_sword" 
    enchanted true 
    rarity "uncommon" 
    lore (literal)"A very cool sword that makes explosions or smth like that"
}

item "test:combustable_powder" {
    replacement (group)"fuels" // Items can also have groups, for those a random item out of the group will be picked
}
```
