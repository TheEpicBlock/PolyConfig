package nl.theepicblock.polyconfig.block;

import io.github.theepicblock.polymc.api.block.BlockStateProfile;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class BlockGroupUtil {
    /**
     * These names are for compatibility reasons
     */
    private static final Map<String,BlockStateProfile> COMPAT_NAMES = new HashMap<>();
    static {
        COMPAT_NAMES.put("saplings", BlockStateProfile.SAPLING_SUB_PROFILE);
        COMPAT_NAMES.put("sugarcane", BlockStateProfile.SUGARCANE_SUB_PROFILE);
        COMPAT_NAMES.put("tripwire", BlockStateProfile.TRIPWIRE_SUB_PROFILE);
        COMPAT_NAMES.put("drip_leaf", BlockStateProfile.SMALL_DRIPLEAF_SUB_PROFILE);
        COMPAT_NAMES.put("cave_vines", BlockStateProfile.CAVE_VINES_SUB_PROFILE);
        COMPAT_NAMES.put("nether_vines", BlockStateProfile.NETHER_VINES_SUB_PROFILE);
        COMPAT_NAMES.put("kelp", BlockStateProfile.KELP_SUB_PROFILE);
        COMPAT_NAMES.put("note_block", BlockStateProfile.NOTE_BLOCK_SUB_PROFILE);
        COMPAT_NAMES.put("target_block", BlockStateProfile.TARGET_BLOCK_SUB_PROFILE);
        COMPAT_NAMES.put("dispensers_and_droppers", BlockStateProfile.DISPENSER_SUB_PROFILE);
        COMPAT_NAMES.put("tnt", BlockStateProfile.TNT_SUB_PROFILE);
        COMPAT_NAMES.put("jukebox", BlockStateProfile.JUKEBOX_SUB_PROFILE);
        COMPAT_NAMES.put("beehives", BlockStateProfile.BEEHIVE_SUB_PROFILE);
        COMPAT_NAMES.put("snowy_grasses", BlockStateProfile.SNOWY_GRASS_SUB_PROFILE);
        COMPAT_NAMES.put("slabs", BlockStateProfile.DOUBLE_SLAB_SUB_PROFILE);
        COMPAT_NAMES.put("waterlogged_slabs", BlockStateProfile.WATERLOGGED_SLAB_SUB_PROFILE);
        COMPAT_NAMES.put("waxed_copper_fullblocks", BlockStateProfile.WAXED_COPPER_FULLBLOCK_SUB_PROFILE);
        COMPAT_NAMES.put("infested_stones", BlockStateProfile.INFESTED_STONE_SUB_PROFILE);
        COMPAT_NAMES.put("petrified_oak_slabs", BlockStateProfile.PETRIFIED_OAK_SLAB_SUB_PROFILE);
        COMPAT_NAMES.put("waxed_copper_slabs", BlockStateProfile.WAXED_COPPER_SLAB_SUB_PROFILE);
        COMPAT_NAMES.put("open_fence_gates", BlockStateProfile.OPEN_FENCE_GATE_PROFILE);
        COMPAT_NAMES.put("fence_gates", BlockStateProfile.FENCE_GATE_PROFILE);
        COMPAT_NAMES.put("pressure_plates", BlockStateProfile.PRESSURE_PLATE_PROFILE);
        COMPAT_NAMES.put("empty_walls", BlockStateProfile.NO_COLLISION_WALL_PROFILE);
        COMPAT_NAMES.put("fullblocks", BlockStateProfile.FULL_BLOCK_PROFILE);
        COMPAT_NAMES.put("climbables", BlockStateProfile.CLIMBABLE_PROFILE);
    }

    public static Optional<BlockStateProfile> tryGet(String name) {
        return BlockStateProfile.ALL_PROFILES.stream()
                .filter(profile -> profile.name.equals(name))
                .findFirst()
                .or(() -> Optional.ofNullable(COMPAT_NAMES.get(name)));
    }
}
