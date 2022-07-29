package nl.theepicblock.polyconfig.block;

import io.github.theepicblock.polymc.api.block.BlockStateProfile;

public enum BlockGroup {
    SAPLINGS(BlockStateProfile.SAPLING_SUB_PROFILE, "saplings"),
    SUGARCANE(BlockStateProfile.SUGARCANE_SUB_PROFILE, "sugarcane"),
    TRIPWIRE(BlockStateProfile.TRIPWIRE_SUB_PROFILE, "tripwire"),
    SMALL_DRIPLEAF(BlockStateProfile.SMALL_DRIPLEAF_SUB_PROFILE, "drip_leaf"),
    CAVE_VINES(BlockStateProfile.CAVE_VINES_SUB_PROFILE, "cave_vines"),
    NETHER_VINES(BlockStateProfile.NETHER_VINES_SUB_PROFILE, "nether_vines"),
    KELP(BlockStateProfile.KELP_SUB_PROFILE, "kelp"),
    NOTEBLOCK(BlockStateProfile.NOTE_BLOCK_SUB_PROFILE, "note_block"),
    TARGET_BLOCK(BlockStateProfile.TARGET_BLOCK_SUB_PROFILE, "target_block"),
    DISPENSERS_AND_DROPPERS(BlockStateProfile.DISPENSER_SUB_PROFILE, "dispensers_and_droppers"),
    TNT(BlockStateProfile.TNT_SUB_PROFILE, "tnt"),
    JUKEBOX(BlockStateProfile.JUKEBOX_SUB_PROFILE, "jukebox"),
    BEEHIVES(BlockStateProfile.BEEHIVE_SUB_PROFILE, "beehives"),
    SNOWY_GRASSES(BlockStateProfile.SNOWY_GRASS_SUB_PROFILE, "snowy_grasses"),
    DOUBLE_SLABS(BlockStateProfile.DOUBLE_SLAB_SUB_PROFILE, "slabs"),
    WATERLOGGED_SLABS(BlockStateProfile.WATERLOGGED_SLAB_SUB_PROFILE, "waterlogged_slabs"),
    WAXED_COPPER_FULLBLOCKS(BlockStateProfile.WAXED_COPPER_FULLBLOCK_SUB_PROFILE, "waxed_copper_fullblocks"),
    INFESTED_STONES(BlockStateProfile.INFESTED_STONE_SUB_PROFILE, "infested_stones"),
    PETRIFIED_OAK_SLABS(BlockStateProfile.PETRIFIED_OAK_SLAB_SUB_PROFILE, "petrified_oak_slabs"),
    WAXED_COPPER_SLABS(BlockStateProfile.WAXED_COPPER_SLAB_SUB_PROFILE, "waxed_copper_slabs"),
    OPEN_FENCE_GATES(BlockStateProfile.OPEN_FENCE_GATE_PROFILE, "open_fence_gates"),
    FENCE_GATES(BlockStateProfile.FENCE_GATE_PROFILE, "fence_gates"),
    PRESSURE_PLATES(BlockStateProfile.PRESSURE_PLATE_PROFILE, "pressure_plates"),
    CACTUS(BlockStateProfile.CACTUS_PROFILE, "cactus"),
    LEAVES(BlockStateProfile.LEAVES_PROFILE, "leaves"),
    WALLS(BlockStateProfile.NO_COLLISION_WALL_PROFILE, "empty_walls"),

    FULL_BLOCKS(BlockStateProfile.FULL_BLOCK_PROFILE, "fullblocks"),
    CLIMBABLES(BlockStateProfile.CLIMBABLE_PROFILE, "climbables");

    public final String name;
    public final BlockStateProfile profile;
    BlockGroup(BlockStateProfile profile, String name) {
        this.name = name;
        this.profile = profile;
    }
}
